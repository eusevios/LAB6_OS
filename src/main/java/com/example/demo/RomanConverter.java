package com.example.demo;

public class RomanConverter {

    // Массивы для хранения соответствий арабских и римских цифр
    private static final int[] arabicValues = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] romanSymbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    // Метод для конвертации арабских цифр в римские
    public static String toRoman(int number) {
        StringBuilder roman = new StringBuilder();
        int i = 0;

        while (number > 0) {
            if (number >= arabicValues[i]) {
                roman.append(romanSymbols[i]);
                number -= arabicValues[i];
            } else {
                i++;
            }
        }

        return roman.toString();
    }
}