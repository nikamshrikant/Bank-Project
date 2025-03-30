import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// Custom Exceptions
class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

class InvalidAmountException extends Exception {
    public InvalidAmountException(String message) {
        super(message);
    }
}

class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String message) {
        super(message);
    }
}

// Transaction Record
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
    private List<Transaction> transactions;
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
        if (amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be positive");
        }
        balance += amount;
        transactions.add(new Transaction("DEPOSIT", amount, "Deposit to account"));
    }

    public synchronized void withdraw(float amount) throws InsufficientFundsException, InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be positive");
        }
        if (balance - amount < MIN_BALANCE) {
            throw new InsufficientFundsException("Insufficient funds. Minimum balance must be $" + MIN_BALANCE);
        }
        balance -= amount;
        transactions.add(new Transaction("WITHDRAWAL", -amount, "Withdrawal from account"));
    }

    // Getters
    public static long getNextAccountNumber() {
        return nextAccountNumber;
    }

    public static void setNextAccountNumber(long next) {
        nextAccountNumber = next;
    }

    public long getAccountNumber() {
        return accountNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public float getBalance() {
        return balance;
    }

    public List<Transaction> getTransactionHistory() {
        return Collections.unmodifiableList(transactions);
    }

    @Override
    public String toString() {
        return String.format("Account Number: %d\nName: %s %s\nBalance: $%.2f",
                accountNumber, firstName, lastName, balance);
    }
}

// Bank Class
class Bank {
    private ConcurrentMap<Long, Account> accounts = new ConcurrentHashMap<>();
    private static final String DATA_FILE = "Bank.data";
    private static final String ENCRYPTION_KEY = "MySecretKey123";

    public Bank() {
        loadAccounts();
    }

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
                    Account acc = new Account(
                            accNo,
                            parts[1],
                            parts[2],
                            Float.parseFloat(parts[3])
                    );
                    accounts.put(accNo, acc);
                    if (accNo > maxAccNumber) {
                        maxAccNumber = accNo;
                    }
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

    public synchronized void deposit(long accountNumber, float amount) 
            throws AccountNotFoundException, InvalidAmountException {
        Account acc = getAccount(accountNumber);
        acc.deposit(amount);
        saveAccounts();
    }

    public synchronized void withdraw(long accountNumber, float amount) 
            throws AccountNotFoundException, InsufficientFundsException, InvalidAmountException {
        Account acc = getAccount(accountNumber);
        acc.withdraw(amount);
        saveAccounts();
    }

    public synchronized void closeAccount(long accountNumber) throws AccountNotFoundException {
        Account acc = accounts.remove(accountNumber);
        if (acc == null) throw new AccountNotFoundException("Account not found: " + accountNumber);
        saveAccounts();
    }

    public void printAllAccounts() {
        accounts.forEach((id, acc) -> System.out.println(acc + "\n"));
    }
}

// Simple Encryption
class SimpleEncryption {
    public static String encrypt(String input, String key) {
        // Implementation placeholder
        return input;
    }

    public static String decrypt(String input, String key) {
        // Implementation placeholder
        return input;
    }
}

// Main Class
public class BankingSystem2 {
    public static void main(String[] args) {
        Bank bank = new Bank();
        Scanner scanner = new Scanner(System.in);
        int choice;

        System.out.println("=== Advanced Banking System ===");
        while (true) {
            printMenu();
            try {
                choice = Integer.parseInt(scanner.nextLine());
                switch (choice) {
                    case 1 -> openAccount(bank, scanner);
                    case 2 -> balanceEnquiry(bank, scanner);
                    case 3 -> deposit(bank, scanner);
                    case 4 -> withdraw(bank, scanner);
                    case 5 -> closeAccount(bank, scanner);
                    case 6 -> bank.printAllAccounts();
                    case 7 -> {
                        System.out.println("Exiting system...");
                        return;
                    }
                    default -> System.out.println("Invalid choice!");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input format!");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n1. Open Account");
        System.out.println("2. Balance Enquiry");
        System.out.println("3. Deposit");
        System.out.println("4. Withdraw");
        System.out.println("5. Close Account");
        System.out.println("6. Show All Accounts");
        System.out.println("7. Exit");
        System.out.print("Enter choice: ");
    }

    private static void openAccount(Bank bank, Scanner scanner) {
        try {
            System.out.print("Enter First Name: ");
            String fname = scanner.nextLine().trim();
            System.out.print("Enter Last Name: ");
            String lname = scanner.nextLine().trim();
            System.out.print("Enter Initial Balance: ");
            float balance = Float.parseFloat(scanner.nextLine());

            Account acc = bank.openAccount(fname, lname, balance);
            System.out.println("\nAccount created successfully!\n" + acc);
        } catch (InvalidAmountException e) {
            System.out.println("Account creation failed: " + e.getMessage());
        }
    }

    private static void balanceEnquiry(Bank bank, Scanner scanner) {
        try {
            System.out.print("Enter Account Number: ");
            long accNum = Long.parseLong(scanner.nextLine());
            Account acc = bank.getAccount(accNum);
            System.out.println("\nAccount Details:\n" + acc);
            System.out.println("\nTransaction History:");
            acc.getTransactionHistory().forEach(System.out::println);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void deposit(Bank bank, Scanner scanner) {
        try {
            System.out.print("Enter Account Number: ");
            long accNum = Long.parseLong(scanner.nextLine());
            System.out.print("Enter Amount: ");
            float amount = Float.parseFloat(scanner.nextLine());
            
            bank.deposit(accNum, amount);
            System.out.println("Deposit successful!");
        } catch (Exception e) {
            System.out.println("Deposit failed: " + e.getMessage());
        }
    }

    private static void withdraw(Bank bank, Scanner scanner) {
        try {
            System.out.print("Enter Account Number: ");
            long accNum = Long.parseLong(scanner.nextLine());
            System.out.print("Enter Amount: ");
            float amount = Float.parseFloat(scanner.nextLine());
            
            bank.withdraw(accNum, amount);
            System.out.println("Withdrawal successful!");
        } catch (Exception e) {
            System.out.println("Withdrawal failed: " + e.getMessage());
        }
    }

    private static void closeAccount(Bank bank, Scanner scanner) {
        try {
            System.out.print("Enter Account Number: ");
            long accNum = Long.parseLong(scanner.nextLine());
            bank.closeAccount(accNum);
            System.out.println("Account closed successfully!");
        } catch (Exception e) {
            System.out.println("Account closure failed: " + e.getMessage());
        }
    }
}