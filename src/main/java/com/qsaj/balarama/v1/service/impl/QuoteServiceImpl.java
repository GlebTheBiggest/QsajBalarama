package com.qsaj.balarama.v1.service.impl;

import com.qsaj.balarama.v1.service.QuoteService;
import java.util.Random;

public class QuoteServiceImpl implements QuoteService {
    private final String[] quotes = {
            "“Beware of little expenses. A small leak will sink a great ship.”",
            "“Money is a terrible master but an excellent servant.”",
            "“It’s not how much money you make, but how much money you keep.”",
            "“A budget is telling your money where to go instead of wondering where it went.”"
    };
    private final Random random = new Random();

    @Override
    public String getRandomQuote() {
        return quotes[random.nextInt(quotes.length)];
    }
}