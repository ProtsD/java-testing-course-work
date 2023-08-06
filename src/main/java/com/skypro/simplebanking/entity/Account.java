package com.skypro.simplebanking.entity;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "accounts")
public class Account {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account-sequence")
  @SequenceGenerator(name = "account-sequence", sequenceName = "account_sequence")
  private Long id;

  private AccountCurrency accountCurrency;
  private Long amount;

  @ManyToOne(optional = false)
  @JoinColumn(nullable = false, updatable = false, name = "user_id")
  private User user;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public AccountCurrency getAccountCurrency() {
    return accountCurrency;
  }

  public void setAccountCurrency(AccountCurrency accountCurrency) {
    this.accountCurrency = accountCurrency;
  }

  public Long getAmount() {
    return amount;
  }

  public void setAmount(Long amount) {
    this.amount = amount;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Account account = (Account) o;
    return Objects.equals(id, account.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Account{" +
            "id=" + id +
            ", accountCurrency=" + accountCurrency +
            ", amount=" + amount +
            ", user=" + user +
            '}';
  }
}
