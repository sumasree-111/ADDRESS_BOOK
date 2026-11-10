package com.addressly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class AddresslyApplication {

    public static void main(String[] args) {
        SpringApplication.run(AddresslyApplication.class, args);
    }

    @Bean
    CommandLineRunner initializeDatabase(JdbcTemplate db) {
        return args -> {
            db.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    phone TEXT NOT NULL UNIQUE,
                    email TEXT
                )
            """);

            db.execute("""
                CREATE TABLE IF NOT EXISTS addresses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    customer_id INTEGER NOT NULL,
                    house TEXT,
                    street TEXT,
                    city TEXT,
                    pin TEXT,
                    is_default INTEGER DEFAULT 0,
                    deleted INTEGER DEFAULT 0,
                    FOREIGN KEY(customer_id)
                    REFERENCES customers(id) ON DELETE CASCADE
                )
            """);

            db.execute("""
                CREATE INDEX IF NOT EXISTS idx_addresses_customer
                ON addresses(customer_id)
            """);

            System.out.println("Addressly database connected successfully!");
        };
    }
}