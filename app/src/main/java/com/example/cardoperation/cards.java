package com.example.cardoperation;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class cards extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_CODE = 1001;
    private TextView resultTextView;
    private RadioGroup cardTypeGroup;
    private Button generateButton;
    private Button downloadButton;
    private Button shareButton;
    private Random random;
    private List<String> generatedNumbers;

    private ImageView cardTypeImage;
    private View cardLayout;
    private TextView validThruText;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cards);

        resultTextView = findViewById(R.id.resultTextView);
        cardTypeGroup = findViewById(R.id.cardTypeGroup);
        generateButton = findViewById(R.id.generateButton);
        downloadButton = findViewById(R.id.downloadButton);
        shareButton = findViewById(R.id.shareButton);
        random = new Random();
        generatedNumbers = new ArrayList<>();

        cardTypeImage = findViewById(R.id.cardTypeImage);
        cardLayout = findViewById(R.id.cardLayout);
        validThruText = findViewById(R.id.validThruText);

        generateButton.setOnClickListener(v -> generateCreditCard());
        downloadButton.setOnClickListener(v -> checkPermissionAndDownload());
        shareButton.setOnClickListener(v -> shareGeneratedNumbers());

        cardTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioVisa) {
                cardTypeImage.setImageResource(R.drawable.ic_visa);
                cardLayout.setBackgroundResource(R.drawable.card_background_visa);
            } else if (checkedId == R.id.radioMastercard) {
                cardTypeImage.setImageResource(R.drawable.ic_mastercard);
                cardLayout.setBackgroundResource(R.drawable.card_background_mastercard);
            } else if (checkedId == R.id.radioAmex) {
                cardTypeImage.setImageResource(R.drawable.ic_amex);
                cardLayout.setBackgroundResource(R.drawable.card_background_amex);
            }
        });
    }

    private void checkPermissionAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermission();
            } else {
                downloadGeneratedNumbers();
            }
        } else {
            downloadGeneratedNumbers();
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadGeneratedNumbers();
            } else {
                Toast.makeText(this, "Storage permission required to download files",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void generateCreditCard() {

        int selectedId = cardTypeGroup.getCheckedRadioButtonId();
        String prefix;
        int length;

        if (selectedId == R.id.radioVisa) {
            prefix = "4";
            length = 16;
        } else if (selectedId == R.id.radioMastercard) {
            prefix = "5" + (random.nextInt(5) + 1);
            length = 16;
        } else if (selectedId == R.id.radioAmex) {
            prefix = "3" + (random.nextInt(2) + 4);
            length = 15;
        } else {
            prefix = "4";
            length = 16;
        }

        String cardNumber = generateValidCreditCardNumber(prefix, length);
        String formattedNumber = formatCardNumber(cardNumber);

        SpannableString spannableString = new SpannableString(formattedNumber);
        spannableString.setSpan(new TypefaceSpan("monospace"),
                0, formattedNumber.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        resultTextView.setText(spannableString);
        int randomYear = Calendar.getInstance().get(Calendar.YEAR) % 100 + 2 + random.nextInt(4);
        int randomMonth = 1 + random.nextInt(12);
        validThruText.setText(String.format(Locale.US, "%02d/%d", randomMonth, randomYear));

        generatedNumbers.add(formattedNumber);
        downloadButton.setEnabled(true);
        shareButton.setEnabled(true);
    }

    private String generateValidCreditCardNumber(String prefix, int length) {
        StringBuilder number = new StringBuilder(prefix);

        for (int i = prefix.length(); i < length - 1; i++) {
            number.append(random.nextInt(10));
        }

        int checkDigit = calculateLuhnCheckDigit(number.toString());
        number.append(checkDigit);

        return number.toString();
    }

    private int calculateLuhnCheckDigit(String partialNumber) {
        int sum = 0;
        boolean alternate = false;

        for (int i = partialNumber.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(partialNumber.charAt(i));
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            alternate = !alternate;
        }

        return (10 - (sum % 10)) % 10;
    }

    private String formatCardNumber(String number) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < number.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append(" ");
            }
            formatted.append(number.charAt(i));
        }
        return formatted.toString();
    }

    private void downloadGeneratedNumbers() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            String fileName = "credit_cards_" + timestamp + ".txt";

            File downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, fileName);

            FileWriter writer = new FileWriter(file);
            writer.write("Generated Credit Card Numbers:\n\n");
            for (String number : generatedNumbers) {
                writer.write(number + "\n");
            }
            writer.close();

            Toast.makeText(this, "File saved to Downloads: " + fileName,
                    Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Toast.makeText(this, "Error saving file: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void shareGeneratedNumbers() {
        StringBuilder content = new StringBuilder();
        content.append("Generated Credit Card Numbers:\n\n");
        for (String number : generatedNumbers) {
            content.append(number).append("\n");
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Generated Credit Card Numbers");
        shareIntent.putExtra(Intent.EXTRA_TEXT, content.toString());
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }
}