package com.project.pawn;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Reistration {
  public static void main(String[] args) {
    String raw = "user123";               // your natural language password
    PasswordEncoder enc = new BCryptPasswordEncoder(10); // 10 is strength (work factor)
    String hash = enc.encode(raw);
    System.out.println(hash);
  }
}

