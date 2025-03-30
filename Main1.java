import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.*;
import java.awt.event.ActionEvent;

// Exception Classes
class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) { super(message); }
}

class InvalidAmountException extends Exception {
    public InvalidAmountException(String message) { super(message); }
}

class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String message) { super(message); }
}

// Transaction Class
class Transaction {
    private String type;
    private float amount;
    private Date timestamp;
    private String description;

    public Transaction(String type, float amount, String description) {
        this.type = type;
        this.amount = amount;
        this.timestamp = new Date();
        this.description = description;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: $%.2f - %s",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp),
                type, amount, description);
    }
}

// Account Class
class Account {
    
    private long accountNumber;
    private String firstName;
    private String lastName;
    private float balance;
    private java.util.List<Transaction> transactions;
    private static long nextAccountNumber = 0;
    public static final float MIN_BALANCE = 500.0f;
    
       
    

    public Account(String firstName, String lastName, float balance) throws InvalidAmountException {
        validateName(firstName);
        validateName(lastName);
        validateBalance(balance);

        this.accountNumber = ++nextAccountNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.balance = balance;
        this.transactions = new ArrayList<>();
        transactions.add(new Transaction("ACCOUNT CREATION", balance, "Initial deposit"));
    }

    public Account(long accountNumber, String firstName, String lastName, float balance) throws InvalidAmountException {
        validateName(firstName);
        validateName(lastName);
        validateBalance(balance);

        this.accountNumber = accountNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.balance = balance;
        this.transactions = new ArrayList<>();
        transactions.add(new Transaction("ACCOUNT CREATION", balance, "Loaded from file"));
    }

    private void validateName(String name) {
        if (!name.matches("^[A-Za-z \\-']+$")) {
            throw new IllegalArgumentException("Invalid name format");
        }
    }

    private void validateBalance(float balance) throws InvalidAmountException {
        if (balance < MIN_BALANCE) {
            throw new InvalidAmountException("Initial balance must be at least $" + MIN_BALANCE);
        }
    }

    public synchronized void deposit(float amount) throws InvalidAmountException {
        if (amount <= 0) throw new InvalidAmountException("Deposit amount must be positive");
        balance += amount;
        transactions.add(new Transaction("DEPOSIT", amount, "Deposit to account"));
    }

    public synchronized void withdraw(float amount) throws InsufficientFundsException, InvalidAmountException {
        if (amount <= 0) throw new InvalidAmountException("Withdrawal amount must be positive");
        if (balance - amount < MIN_BALANCE) throw new InsufficientFundsException("Insufficient funds. Minimum balance must be $" + MIN_BALANCE);
        balance -= amount;
        transactions.add(new Transaction("WITHDRAWAL", -amount, "Withdrawal from account"));
    }

    public String getFullTransactionHistory() {
        StringBuilder sb = new StringBuilder();
        for (Transaction t : this.transactions) {
            sb.append(t.toString()).append("\n");
        }
        return sb.toString();
    }

    public static void setNextAccountNumber(long next) { nextAccountNumber = next; }
    public long getAccountNumber() { return accountNumber; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public float getBalance() { return balance; }

    @Override
    public String toString() {
        return String.format("Account Number: %d\nName: %s %s\nBalance: $%.2f",
                accountNumber, firstName, lastName, balance);
    }
}

// Bank Class
class Bank {
    private final ConcurrentMap<Long, Account> accounts = new ConcurrentHashMap<>();
    private static final String DATA_FILE = "Bank.data";
    private static final String ENCRYPTION_KEY = "MySecretKey123";

    public Bank() { loadAccounts(); }

    private void loadAccounts() {
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            long maxAccNumber = 0;
            while ((line = reader.readLine()) != null) {
                try {
                    String decryptedLine = SimpleEncryption.decrypt(line, ENCRYPTION_KEY);
                    String[] parts = decryptedLine.split(",");
                    if (parts.length != 4) continue;

                    long accNo = Long.parseLong(parts[0]);
                    Account acc = new Account(accNo, parts[1], parts[2], Float.parseFloat(parts[3]));
                    accounts.put(accNo, acc);
                    if (accNo > maxAccNumber) maxAccNumber = accNo;
                } catch (Exception e) { 
                    System.err.println("Error loading account: " + e.getMessage()); 
                }
            }
            Account.setNextAccountNumber(maxAccNumber);
        } catch (IOException e) { 
            System.out.println("No existing data file found. Starting fresh."); 
        }
    }

    private void saveAccounts() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DATA_FILE))) {
            for (Account acc : accounts.values()) {
                String data = String.format("%d,%s,%s,%.2f",
                        acc.getAccountNumber(),
                        acc.getFirstName(),
                        acc.getLastName(),
                        acc.getBalance());
                writer.println(SimpleEncryption.encrypt(data, ENCRYPTION_KEY));
            }
        } catch (Exception e) { 
            System.err.println("Error saving accounts: " + e.getMessage()); 
        }
    }

    public synchronized Account openAccount(String firstName, String lastName, float balance) throws InvalidAmountException {
        Account acc = new Account(firstName, lastName, balance);
        accounts.put(acc.getAccountNumber(), acc);
        saveAccounts();
        return acc;
    }

    public synchronized Account getAccount(long accountNumber) throws AccountNotFoundException {
        Account acc = accounts.get(accountNumber);
        if (acc == null) throw new AccountNotFoundException("Account not found: " + accountNumber);
        return acc;
    }

    public synchronized void deposit(long accountNumber, float amount) throws AccountNotFoundException, InvalidAmountException {
        Account acc = getAccount(accountNumber);
        acc.deposit(amount);
        saveAccounts();
    }

    public synchronized void withdraw(long accountNumber, float amount) throws AccountNotFoundException, InsufficientFundsException, InvalidAmountException {
        Account acc = getAccount(accountNumber);
        acc.withdraw(amount);
        saveAccounts();
    }

    public synchronized void closeAccount(long accountNumber) throws AccountNotFoundException {
        Account acc = accounts.remove(accountNumber);
        if (acc == null) throw new AccountNotFoundException("Account not found: " + accountNumber);
        saveAccounts();
    }

    public String getAllAccountsAsString() {
        StringBuilder sb = new StringBuilder();
        accounts.forEach((id, acc) -> sb.append(acc).append("\n\n"));
        return sb.toString();
    }
}

// GUI Class
class BankGUI extends JFrame {
    private final Bank bank = new Bank();

    public BankGUI() {
        initializeUI();
        setVisible(true);
    }

    private void initializeUI() {
        setTitle("Banking System GUI");
        setSize(600, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(7, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        String[] buttons = {
            "Open New Account", "View Account Balance", 
            "Make Deposit", "Make Withdrawal",
            "Close Account", "Show All Accounts", "Exit"
        };

        for (String btnText : buttons) {
            JButton btn = new JButton(btnText);
            btn.addActionListener(this::handleButtonAction);
            panel.add(btn);
        }

        add(panel);
    }

    private void handleButtonAction(ActionEvent e) {
        String command = ((JButton) e.getSource()).getText();
        try {
            switch (command) {
                case "Open New Account":
                    openAccount();
                    break;
                case "View Account Balance":
                    viewBalance();
                    break;
                case "Make Deposit":
                    performTransaction("Deposit");
                    break;
                case "Make Withdrawal":
                    performTransaction("Withdraw");
                    break;
                case "Close Account":
                    closeAccount();
                    break;
                case "Show All Accounts":
                    showAllAccounts();
                    break;
                case "Exit":
                    System.exit(0);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid command");
            }
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void openAccount() {
        JTextField firstName = new JTextField();
        JTextField lastName = new JTextField();
        JTextField balance = new JTextField();

        Object[] message = {
            "First Name:", firstName,
            "Last Name:", lastName,
            "Initial Balance:", balance
        };

        int option = JOptionPane.showConfirmDialog(
            this, message, "Open New Account", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            try {
                Account acc = bank.openAccount(
                    firstName.getText().trim(),
                    lastName.getText().trim(),
                    Float.parseFloat(balance.getText())
                );
                showMessage("Account Created Successfully!\n" + acc);
            } catch (InvalidAmountException | NumberFormatException e) {
                showError("Account creation failed: " + e.getMessage());
            }
        }
    }

    private void viewBalance() {
        long accNumber = promptAccountNumber();
        if (accNumber == -1) return;

        try {
            Account acc = bank.getAccount(accNumber);
            JTextArea textArea = new JTextArea(15, 40);
            textArea.setEditable(false);
            textArea.append("Account Details:\n" + acc + "\n\nTransaction History:\n");
            textArea.append(acc.getFullTransactionHistory());
            JScrollPane scrollPane = new JScrollPane(textArea);
            JOptionPane.showMessageDialog(this, scrollPane, "Account Details", JOptionPane.INFORMATION_MESSAGE);
        } catch (AccountNotFoundException e) {
            showError(e.getMessage());
        }
    }

    private void performTransaction(String type) {
        long accNumber = promptAccountNumber();
        if (accNumber == -1) return;

        JTextField amountField = new JTextField();
        Object[] message = {"Amount:", amountField};
        
        int option = JOptionPane.showConfirmDialog(
            this, message, type, JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION) {
            try {
                float amount = Float.parseFloat(amountField.getText());
                if ("Deposit".equals(type)) {
                    bank.deposit(accNumber, amount);
                } else {
                    bank.withdraw(accNumber, amount);
                }
                showMessage("Transaction completed successfully!");
            } catch (NumberFormatException e) {
                showError("Invalid number format");
            } catch (AccountNotFoundException | InvalidAmountException | InsufficientFundsException e) {
                showError(e.getMessage());
            }
        }
    }

    private void closeAccount() {
        long accNumber = promptAccountNumber();
        if (accNumber == -1) return;

        try {
            bank.closeAccount(accNumber);
            showMessage("Account closed successfully");
        } catch (AccountNotFoundException e) {
            showError(e.getMessage());
        }
    }

    private void showAllAccounts() {
        JTextArea textArea = new JTextArea(20, 50);
        textArea.setEditable(false);
        textArea.setText(bank.getAllAccountsAsString());
        JScrollPane scrollPane = new JScrollPane(textArea);
        JOptionPane.showMessageDialog(this, scrollPane, "All Accounts", JOptionPane.INFORMATION_MESSAGE);
    }

    private long promptAccountNumber() {
        JTextField accField = new JTextField();
        Object[] message = {"Enter Account Number:", accField};
        int option = JOptionPane.showConfirmDialog(
            this, message, "Account Number", JOptionPane.OK_CANCEL_OPTION);
        
        if (option == JOptionPane.OK_OPTION) {
            try {
                return Long.parseLong(accField.getText());
            } catch (NumberFormatException e) {
                showError("Invalid account number format");
            }
        }
        return -1;
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private void showError(String error) {
        JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
    }
}

// Encryption Class
class SimpleEncryption {
    public static String encrypt(String input, String key) {
        // Basic XOR encryption for demonstration
        char[] chars = input.toCharArray();
        char[] keyChars = key.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) (chars[i] ^ keyChars[i % keyChars.length]);
        }
        return new String(chars);
    }

    public static String decrypt(String input, String key) {
        // XOR decryption (same as encryption)
        return encrypt(input, key);
    }
}

public class Main1 {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BankGUI().setVisible(true));
    }
}