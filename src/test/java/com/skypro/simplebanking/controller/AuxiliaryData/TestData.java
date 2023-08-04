package com.skypro.simplebanking.controller.AuxiliaryData;

import com.skypro.simplebanking.dto.BankingUserDetails;
import com.skypro.simplebanking.dto.TransferRequest;
import com.skypro.simplebanking.entity.Account;
import com.skypro.simplebanking.entity.AccountCurrency;
import com.skypro.simplebanking.entity.User;
import com.skypro.simplebanking.repository.AccountRepository;
import com.skypro.simplebanking.repository.UserRepository;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class TestData {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public TestData(
            UserRepository userRepository,
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${test.database.users-number}") int number) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        createUsers(number);
    }

    private void createUsers(int size) {
        List<User> userList = new ArrayList<>();

        while (size > userList.size()) {
            String userName = "User_" + (userList.size() + 1);
            String password = "User_" + (userList.size() + 1) + "_password";
            int refNumber = 10 * (userList.size() + 1);

            User user = new User();
            user.setUsername(userName);
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
            createAccounts(user, refNumber);
            userList.add(user);
        }
    }

    private void createAccounts(User user, int refNumber) {
        user.setAccounts(new ArrayList<>());
        for (
                AccountCurrency currency : AccountCurrency.values()) {
            Account account = new Account();
            account.setUser(user);
            account.setAccountCurrency(currency);
            account.setAmount((long) (currency.ordinal() + refNumber));
            user.getAccounts().add(account);
            accountRepository.save(account);
        }
    }

    public JSONObject generateNewUser() {
        long UserNumber = userRepository.findAll().stream()
                .sorted((e1, e2) -> e2.getId().compareTo(e1.getId()))
                .findFirst()
                .orElseThrow()
                .getId()
                + 1L;

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("username", "User_" + UserNumber);
        jsonObject.put("password", "User_" + UserNumber + "_password");
        return jsonObject;
    }

    public Account findRandomAccount() {
        Random random = new Random();
        List<Account> accounts = accountRepository.findAll();
        return accounts.get(random.nextInt(accounts.size()));
    }

    public List<Account> findTwoRandomAccounts() {
        Random random = new Random();
        List<Account> randomAccounts = new ArrayList<>();
        List<Account> accounts = accountRepository.findAll();
        randomAccounts.add(accounts.get(random.nextInt(accounts.size())));
        accounts = accounts
                .stream()
                .filter(e -> !e.equals(randomAccounts.get(0)))
                .filter(e -> e.getAccountCurrency().equals(randomAccounts.get(0).getAccountCurrency()))
                .collect(Collectors.toList());
        randomAccounts.add(accounts.get(random.nextInt(accounts.size())));
        return randomAccounts;
    }

    public TransferRequest createTransferRequest(Account account1, Account account2) {
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFromAccountId(account1.getId());
        transferRequest.setToAccountId(account2.getId());
        transferRequest.setToUserId(account2.getUser().getId());
        transferRequest.setAmount(account1.getAmount() / 2);
        return transferRequest;
    }

    public BankingUserDetails getAuthUser(long id) {
        User user = userRepository.findById(id).orElseThrow();
        return new BankingUserDetails(user.getId(), user.getUsername(), user.getPassword(), false);
    }
}
