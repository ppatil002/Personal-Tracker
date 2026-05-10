package com.example.pp.features.tracker.edit;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.pp.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditTrackerActivity extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private final String[] paymentOptions = {"UPI", "CASH", "ICICI", "ICICI AMA", "AXIS"};
    private String selectedPayment = "UPI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_tracker);

        calendarView = findViewById(R.id.calendarView);

        setupCalendar();
        setupDropdowns();
        setupBottomNavigation();
        setupActions();
    }

    private void setupCalendar() {
        if (calendarView == null) return;

        calendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(MaterialCalendarView widget, CalendarDay date, boolean selected) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(date.getYear(), date.getMonth() - 1, date.getDay());
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
                Toast.makeText(EditTrackerActivity.this, 
                    dateFormat.format(calendar.getTime()) + " - " + dayFormat.format(calendar.getTime()), 
                    Toast.LENGTH_SHORT).show();
            }
        });

        // Set default selected date to today
        Calendar today = Calendar.getInstance();
        calendarView.setSelectedDate(CalendarDay.from(
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH) + 1,
                today.get(Calendar.DAY_OF_MONTH)
        ));
    }

    private void setupDropdowns() {
        View paymentView = findViewById(R.id.paymentDropdown);
        if (paymentView != null) {
            ((TextView) paymentView.findViewById(R.id.dropdownText)).setText(selectedPayment);
            ((ImageView) paymentView.findViewById(R.id.dropdownIcon)).setImageResource(R.drawable.ic_credit_card);
            paymentView.findViewById(R.id.defaultIndicator).setVisibility(View.GONE);

            paymentView.setOnClickListener(v -> showPaymentBottomSheet(paymentView));
        }
    }

    private void showPaymentBottomSheet(View paymentView) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.RoundedBottomSheetDialog);
        dialog.setContentView(R.layout.bottom_sheet_payment_options);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundResource(R.drawable.bg_bottom_sheet_rounded);
        }
        TextView option1 = dialog.findViewById(R.id.optionUPI);
        TextView option2 = dialog.findViewById(R.id.optionCASH);
        TextView option3 = dialog.findViewById(R.id.optionICICI);
        TextView option4 = dialog.findViewById(R.id.optionICICIAMA);
        TextView option5 = dialog.findViewById(R.id.optionAXIS);

        View.OnClickListener optionClick = v -> {
            TextView tv = (TextView) v;
            selectedPayment = tv.getText().toString();
            ((TextView) paymentView.findViewById(R.id.dropdownText)).setText(selectedPayment);
            dialog.dismiss();
        };

        if (option1 != null) option1.setOnClickListener(optionClick);
        if (option2 != null) option2.setOnClickListener(optionClick);
        if (option3 != null) option3.setOnClickListener(optionClick);
        if (option4 != null) option4.setOnClickListener(optionClick);
        if (option5 != null) option5.setOnClickListener(optionClick);

        dialog.show();
    }

    private void setupBottomNavigation() {
        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView btnRefresh = findViewById(R.id.btnRefresh);
        ImageButton btnCenterFab = findViewById(R.id.btnCenterFab);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnRefresh != null) btnRefresh.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
        });
        if (btnCenterFab != null) btnCenterFab.setOnClickListener(v -> {
            Toast.makeText(this, "Add new entry", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.navDashboard).setOnClickListener(v -> Toast.makeText(this, "Dashboard", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navAnalytics).setOnClickListener(v -> Toast.makeText(this, "Analytics", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navHistory).setOnClickListener(v -> Toast.makeText(this, "History", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navMore).setOnClickListener(v -> Toast.makeText(this, "More", Toast.LENGTH_SHORT).show());
    }

    private void setupActions() {
        findViewById(R.id.btnDoneForDay).setOnClickListener(v -> Toast.makeText(this, "Marked done for the day", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> Toast.makeText(this, "Entry saved", Toast.LENGTH_SHORT).show());

        // Type toggle logic
        View tvTypeExpense = findViewById(R.id.tvTypeExpense);
        View tvTypeRefund = findViewById(R.id.tvTypeRefund);

        if (tvTypeExpense != null && tvTypeRefund != null) {
            tvTypeExpense.setOnClickListener(v -> {
                tvTypeExpense.setBackgroundResource(R.drawable.bg_type_selected);
                ((TextView) tvTypeExpense).setTextColor(ContextCompat.getColor(this, R.color.white));
                tvTypeRefund.setBackground(null);
                ((TextView) tvTypeRefund).setTextColor(ContextCompat.getColor(this, R.color.text_muted));
            });

            tvTypeRefund.setOnClickListener(v -> {
                tvTypeRefund.setBackgroundResource(R.drawable.bg_type_selected);
                ((TextView) tvTypeRefund).setTextColor(ContextCompat.getColor(this, R.color.white));
                tvTypeExpense.setBackground(null);
                ((TextView) tvTypeExpense).setTextColor(ContextCompat.getColor(this, R.color.text_muted));
            });
        }
    }
}