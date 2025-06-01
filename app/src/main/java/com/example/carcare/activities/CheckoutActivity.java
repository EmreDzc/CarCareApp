package com.example.carcare.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.Calendar;
import java.util.Date;

import com.example.carcare.R;
import com.example.carcare.StoreActivity;
import com.example.carcare.models.Product;
import com.example.carcare.models.AddressModel;
import com.example.carcare.models.CardModel;
import com.example.carcare.utils.Cart;
import com.example.carcare.adapters.OrderSummaryAdapter;
import com.example.carcare.ProfilePage.address.AddEditAddressActivity;
import com.example.carcare.ProfilePage.card.AddEditCardActivity;
import com.example.carcare.CardSelectionActivity;
import com.example.carcare.AddressSelectionActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {
    private static final String TAG = "CheckoutActivity";
    private static final int REQUEST_ADDRESS_SELECTION = 1001;
    private static final int REQUEST_ADD_ADDRESS = 1002;
    private static final int REQUEST_CARD_SELECTION = 1003;

    // Adres seçimi için view'lar
    private MaterialCardView layoutDeliveryAddress;
    private TextView textSelectedAddressTitle;
    private TextView textSelectedAddressLine1;
    private TextView textSelectedAddressLine2;
    private TextView textSelectedRecipientInfo;
    private TextView textAddEditAddress;

    // Kart seçimi için view'lar
    private MaterialCardView layoutCardSelection;
    private TextView textSelectedCardName;
    private TextView textSelectedCardNumber;
    private TextView textSelectedCardHolder;
    private TextView textAddEditCard;

    // Sözleşme checkbox'ları
    private CheckBox checkboxPreInformation;
    private CheckBox checkboxDistanceSales;
    private CheckBox checkboxKvkk;
    private CheckBox checkboxCommunication;
    private TextView textViewPreInformation;
    private TextView textViewDistanceSales;
    private TextView textViewKvkk;

    // Mevcut view'lar
    private RecyclerView recyclerOrderItems;
    private TextView textSubtotal, textTax, textTotal;
    private LinearLayout buttonPayContainer;
    private TextView textButtonTotal;
    private TextView textButtonItemCount;
    private ProgressBar progressBar;
    private ImageButton backButton;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private OrderSummaryAdapter orderAdapter;
    private AddressModel selectedAddress;
    private CardModel selectedCard;

    private static final double TAX_RATE = 0.08;
    private DecimalFormat priceFormat = new DecimalFormat("$0.00", new DecimalFormatSymbols(Locale.US));
    private DecimalFormat tlFormat = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(new Locale("tr", "TR")));

    private LinearLayout layoutPriceDetails;
    private ImageView arrowExpandCollapse;
    private TextView textShipping, textDiscount;
    private LinearLayout layoutDiscount;
    private boolean isPriceDetailsExpanded = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Direkt satın alma kontrolü
        boolean isDirectBuy = getIntent().getBooleanExtra("DIRECT_BUY", false);
        String productId = getIntent().getStringExtra("PRODUCT_ID");

        if (isDirectBuy && productId != null) {
            Log.d(TAG, "Direkt satın alma modu aktif, Product ID: " + productId);
        }

        // Sepet boş kontrolü
        if (Cart.getInstance().getItems().isEmpty() && !isDirectBuy) {
            Toast.makeText(this, "Sepetiniz boş. Mağazaya yönlendiriliyorsunuz.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, StoreActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        initializeViews();
        setupOrderSummary();
        setupEventListeners();
        calculateTotals();
        loadDefaultAddress();
        loadDefaultCard();
    }

    private void initializeViews() {
        // Adres seçimi view'ları
        layoutDeliveryAddress = findViewById(R.id.layout_delivery_address);
        textSelectedAddressTitle = findViewById(R.id.text_selected_address_title);
        textSelectedAddressLine1 = findViewById(R.id.text_selected_address_line1);
        textSelectedAddressLine2 = findViewById(R.id.text_selected_address_line2);
        textSelectedRecipientInfo = findViewById(R.id.text_selected_recipient_info);
        textAddEditAddress = findViewById(R.id.text_add_edit_address);

        // Kart seçimi view'ları
        layoutCardSelection = findViewById(R.id.layout_card_selection);
        textSelectedCardName = findViewById(R.id.text_selected_card_name);
        textSelectedCardNumber = findViewById(R.id.text_selected_card_number);
        textSelectedCardHolder = findViewById(R.id.text_selected_card_holder);
        textAddEditCard = findViewById(R.id.text_add_edit_card);

        // Sözleşme checkbox'ları
        checkboxPreInformation = findViewById(R.id.checkbox_pre_information);
        checkboxDistanceSales = findViewById(R.id.checkbox_distance_sales);
        checkboxKvkk = findViewById(R.id.checkbox_kvkk);
        checkboxCommunication = findViewById(R.id.checkbox_communication);
        textViewPreInformation = findViewById(R.id.text_view_pre_information);
        textViewDistanceSales = findViewById(R.id.text_view_distance_sales);
        textViewKvkk = findViewById(R.id.text_view_kvkk);

        // RecyclerView ve fiyat view'ları
        recyclerOrderItems = findViewById(R.id.recyclerOrderItems);
        textSubtotal = findViewById(R.id.textSubtotal);
        textTotal = findViewById(R.id.textTotal);

        // Yeni bottom bar yapısı
        MaterialButton buttonPay = findViewById(R.id.buttonPay);
        progressBar = findViewById(R.id.progressBar);
        backButton = findViewById(R.id.button_back_to_cart);

        // Açılır kapanır fiyat detayları için view'lar
        layoutPriceDetails = findViewById(R.id.layout_price_details);
        arrowExpandCollapse = findViewById(R.id.arrow_expand_collapse);
        textShipping = findViewById(R.id.textShipping);
        textDiscount = findViewById(R.id.textDiscount);
        layoutDiscount = findViewById(R.id.layout_discount);

        // Geri butonu click listener
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        } else {
            Log.w(TAG, "Geri butonu (button_back_to_cart) layout'ta bulunamadı.");
        }
    }

    private void setupOrderSummary() {
        recyclerOrderItems.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new OrderSummaryAdapter(Cart.getInstance().getItems());
        recyclerOrderItems.setAdapter(orderAdapter);
    }

    private void setupEventListeners() {
        // Adres seçimi click listener
        layoutDeliveryAddress.setOnClickListener(v -> openAddressSelection());

        // Adres ekle/düzenle click listener
        textAddEditAddress.setOnClickListener(v -> {
            if (selectedAddress != null) {
                // Seçili adresi düzenle
                Intent intent = new Intent(this, AddEditAddressActivity.class);
                intent.putExtra("address_id_to_edit", selectedAddress.getDocumentId());
                startActivityForResult(intent, REQUEST_ADD_ADDRESS);
            } else {
                // Yeni adres ekle
                Intent intent = new Intent(this, AddEditAddressActivity.class);
                startActivityForResult(intent, REQUEST_ADD_ADDRESS);
            }
        });

        // Kart seçimi click listener
        layoutCardSelection.setOnClickListener(v -> openCardSelection());

        // Kart ekle/düzenle click listener
        textAddEditCard.setOnClickListener(v -> {
            if (selectedCard != null) {
                // Seçili kartı düzenle veya başka kart seç
                openCardSelection();
            } else {
                // Yeni kart ekle
                Intent intent = new Intent(this, AddEditCardActivity.class);
                startActivityForResult(intent, REQUEST_CARD_SELECTION);
            }
        });

        // Sözleşme linklerini ayarla
        textViewPreInformation.setOnClickListener(v -> showContractDialog("Ön Bilgilendirme Koşulları", getPreInformationText()));
        textViewDistanceSales.setOnClickListener(v -> showContractDialog("Mesafeli Satış Sözleşmesi", getDistanceSalesText()));
        textViewKvkk.setOnClickListener(v -> showContractDialog("KVKK Aydınlatma Metni", getKvkkText()));

        // Fiyat detayları açılır kapanır click listener
        findViewById(R.id.layout_total_price_trigger).setOnClickListener(v -> togglePriceDetails());

        // Yeni MaterialButton click listener
        findViewById(R.id.buttonPay).setOnClickListener(v -> {
            if (validateForm()) {
                processOrder();
            }
        });
    }

    // Yeni metod: Fiyat detaylarını açma/kapama
    private void togglePriceDetails() {
        if (isPriceDetailsExpanded) {
            collapsePriceDetails();
        } else {
            expandPriceDetails();
        }
    }


    // Fiyat detaylarını açma animasyonu
    private void expandPriceDetails() {
        layoutPriceDetails.setVisibility(View.VISIBLE);

        // Height animasyonu
        ValueAnimator animator = slideDown(layoutPriceDetails);
        animator.start();

        // Ok animasyonu
        ObjectAnimator arrowAnimator = ObjectAnimator.ofFloat(arrowExpandCollapse, "rotation", 0f, 180f);
        arrowAnimator.setDuration(300);
        arrowAnimator.start();

        isPriceDetailsExpanded = true;
    }

    // Fiyat detaylarını kapama animasyonu
    private void collapsePriceDetails() {
        // Height animasyonu
        ValueAnimator animator = slideUp(layoutPriceDetails);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                layoutPriceDetails.setVisibility(View.GONE);
            }
        });
        animator.start();

        // Ok animasyonu
        ObjectAnimator arrowAnimator = ObjectAnimator.ofFloat(arrowExpandCollapse, "rotation", 180f, 0f);
        arrowAnimator.setDuration(300);
        arrowAnimator.start();

        isPriceDetailsExpanded = false;
    }

    // Slide down animasyonu
    private ValueAnimator slideDown(View view) {
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int targetHeight = view.getMeasuredHeight();

        ValueAnimator animator = ValueAnimator.ofInt(0, targetHeight);
        animator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = (int) animation.getAnimatedValue();
            view.setLayoutParams(layoutParams);
        });
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        return animator;
    }

    // Slide up animasyonu
    private ValueAnimator slideUp(View view) {
        int initialHeight = view.getMeasuredHeight();

        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, 0);
        animator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = (int) animation.getAnimatedValue();
            view.setLayoutParams(layoutParams);
        });
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateInterpolator());
        return animator;
    }

    private void calculateTotals() {
        double subtotal = Cart.getInstance().getTotalPrice();
        double shipping = 0.0; // Ücretsiz kargo
        double discount = 5.0; // Kupon indirimi
        double total = subtotal + shipping - discount;

        // Fiyat gösterimleri - TL formatında
        textSubtotal.setText(tlFormat.format(subtotal) + " TL");

        if (shipping > 0) {
            textShipping.setText(tlFormat.format(shipping) + " TL");
            textShipping.setTextColor(ContextCompat.getColor(this, R.color.grey_medium));
        } else {
            textShipping.setText("Ücretsiz");
            textShipping.setTextColor(ContextCompat.getColor(this, R.color.green_success));
        }

        textTotal.setText(tlFormat.format(total) + " TL");

        // MaterialButton içindeki fiyat güncellemesi
        MaterialButton buttonPay = findViewById(R.id.buttonPay);
        buttonPay.setText("Onayla ve Bitir • " + tlFormat.format(total) + " TL");

        // İndirim varsa göster
        if (discount > 0) {
            layoutDiscount.setVisibility(View.VISIBLE);
            textDiscount.setText("-" + tlFormat.format(discount) + " TL");
        } else {
            layoutDiscount.setVisibility(View.GONE);
        }
    }

    private void loadDefaultAddress() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showNoAddressState();
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .collection("deliveryAddresses")
                .whereEqualTo("isDefaultAddress", true)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        AddressModel address = queryDocumentSnapshots.getDocuments().get(0).toObject(AddressModel.class);
                        if (address != null) {
                            address.setDocumentId(queryDocumentSnapshots.getDocuments().get(0).getId());
                            selectedAddress = address;
                            updateSelectedAddressUI();
                        }
                    } else {
                        loadFirstAvailableAddress();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Varsayılan adres yüklenirken hata", e);
                    showNoAddressState();
                });
    }

    private void loadFirstAvailableAddress() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showNoAddressState();
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .collection("deliveryAddresses")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        AddressModel address = queryDocumentSnapshots.getDocuments().get(0).toObject(AddressModel.class);
                        if (address != null) {
                            address.setDocumentId(queryDocumentSnapshots.getDocuments().get(0).getId());
                            selectedAddress = address;
                            updateSelectedAddressUI();
                        }
                    } else {
                        showNoAddressState();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Adres yüklenirken hata", e);
                    showNoAddressState();
                });
    }

    private void updateSelectedAddressUI() {
        if (selectedAddress != null) {
            textSelectedAddressTitle.setText(selectedAddress.getTitle());
            textSelectedAddressLine1.setText(selectedAddress.getAddressLine1());
            textSelectedAddressLine2.setText(selectedAddress.getDistrict() + " / " + selectedAddress.getProvince());

            String recipientInfo = selectedAddress.getRecipientName() + " " + selectedAddress.getRecipientSurname();
            if (selectedAddress.getRecipientPhone() != null && !selectedAddress.getRecipientPhone().isEmpty()) {
                recipientInfo += " • " + selectedAddress.getRecipientPhone();
            }
            textSelectedRecipientInfo.setText(recipientInfo);
            textAddEditAddress.setText("Değiştir");
        } else {
            showNoAddressState();
        }
    }

    private void showNoAddressState() {
        textSelectedAddressTitle.setText("Teslimat adresi seçiniz");
        textSelectedAddressLine1.setText("");
        textSelectedAddressLine2.setText("");
        textSelectedRecipientInfo.setText("");
        textAddEditAddress.setText("Ekle");
        selectedAddress = null;
    }

    private void openAddressSelection() {
        Intent intent = new Intent(this, AddressSelectionActivity.class);
        if (selectedAddress != null) {
            intent.putExtra("selected_address_id", selectedAddress.getDocumentId());
        }
        startActivityForResult(intent, REQUEST_ADDRESS_SELECTION);
    }

    private void loadSelectedAddress(String addressId) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .collection("deliveryAddresses")
                .document(addressId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        AddressModel address = documentSnapshot.toObject(AddressModel.class);
                        if (address != null) {
                            address.setDocumentId(documentSnapshot.getId());
                            selectedAddress = address;
                            updateSelectedAddressUI();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Seçili adres yüklenirken hata", e);
                    Toast.makeText(this, "Adres yüklenirken hata oluştu", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadDefaultCard() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showNoCardState();
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .collection("savedCards")
                .whereEqualTo("isDefault", true)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        CardModel card = queryDocumentSnapshots.getDocuments().get(0).toObject(CardModel.class);
                        if (card != null) {
                            card.setDocumentId(queryDocumentSnapshots.getDocuments().get(0).getId());
                            selectedCard = card;
                            updateSelectedCardUI();
                        }
                    } else {
                        loadFirstAvailableCard();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Varsayılan kart yüklenirken hata", e);
                    showNoCardState();
                });
    }

    private void loadFirstAvailableCard() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showNoCardState();
            return;
        }
        db.collection("users")
                .document(user.getUid())
                .collection("savedCards")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        CardModel card = queryDocumentSnapshots.getDocuments().get(0).toObject(CardModel.class);
                        if (card != null) {
                            card.setDocumentId(queryDocumentSnapshots.getDocuments().get(0).getId());
                            selectedCard = card;
                            updateSelectedCardUI();
                        }
                    } else {
                        showNoCardState();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Kart yüklenirken hata", e);
                    showNoCardState();
                });
    }

    private void updateSelectedCardUI() {
        if (selectedCard != null) {
            textSelectedCardName.setText(selectedCard.getCardName());
            textSelectedCardNumber.setText(selectedCard.getMaskedCardNumber());
            textSelectedCardHolder.setText(selectedCard.getCardHolderName());
            textAddEditCard.setText("Başka Kartla Öde");
        } else {
            showNoCardState();
        }
    }

    private void showNoCardState() {
        textSelectedCardName.setText("Kart bilgileri seçiniz");
        textSelectedCardNumber.setText("");
        textSelectedCardHolder.setText("");
        textAddEditCard.setText("Ekle");
        selectedCard = null;
    }

    private void openCardSelection() {
        Intent intent = new Intent(this, CardSelectionActivity.class);
        if (selectedCard != null) {
            intent.putExtra("selected_card_id", selectedCard.getDocumentId());
        }
        startActivityForResult(intent, REQUEST_CARD_SELECTION);
    }

    private void loadSelectedCard(String cardId) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .collection("savedCards")
                .document(cardId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        CardModel card = documentSnapshot.toObject(CardModel.class);
                        if (card != null) {
                            card.setDocumentId(documentSnapshot.getId());
                            selectedCard = card;
                            updateSelectedCardUI();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Seçili kart yüklenirken hata", e);
                    Toast.makeText(this, "Kart yüklenirken hata oluştu", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_ADDRESS_SELECTION) {
                String selectedAddressId = data.getStringExtra("selected_address_id");
                if (selectedAddressId != null) {
                    loadSelectedAddress(selectedAddressId);
                }
            } else if (requestCode == REQUEST_ADD_ADDRESS) {
                loadDefaultAddress();
            } else if (requestCode == REQUEST_CARD_SELECTION) {
                String selectedCardId = data.getStringExtra("selected_card_id");
                if (selectedCardId != null) {
                    loadSelectedCard(selectedCardId);
                } else {
                    loadDefaultCard();
                }
            }
        }
    }

    private boolean validateForm() {
        if (selectedAddress == null) {
            Toast.makeText(this, "Lütfen teslimat adresi seçiniz.", Toast.LENGTH_SHORT).show();
            layoutDeliveryAddress.requestFocus();
            return false;
        }

        if (selectedCard == null) {
            Toast.makeText(this, "Lütfen ödeme kartı seçiniz.", Toast.LENGTH_SHORT).show();
            layoutCardSelection.requestFocus();
            return false;
        }

        if (Cart.getInstance().getItems().isEmpty()) {
            Toast.makeText(this, "Sepetiniz boş! Mağazaya yönlendiriliyorsunuz.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, StoreActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return false;
        }

        if (!checkboxPreInformation.isChecked()) {
            Toast.makeText(this, "Lütfen Ön Bilgilendirme Koşulları'nı onaylayın.", Toast.LENGTH_SHORT).show();
            checkboxPreInformation.requestFocus();
            return false;
        }

        if (!checkboxDistanceSales.isChecked()) {
            Toast.makeText(this, "Lütfen Mesafeli Satış Sözleşmesi'ni onaylayın.", Toast.LENGTH_SHORT).show();
            checkboxDistanceSales.requestFocus();
            return false;
        }

        if (!checkboxKvkk.isChecked()) {
            Toast.makeText(this, "Lütfen KVKK Aydınlatma Metni'ni onaylayın.", Toast.LENGTH_SHORT).show();
            checkboxKvkk.requestFocus();
            return false;
        }
        return true;
    }

    private void processOrder() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Lütfen önce giriş yapın.", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        MaterialButton buttonPay = findViewById(R.id.buttonPay);
        buttonPay.setEnabled(false);

        Map<String, Object> orderData = new HashMap<>();

        // Kullanıcı bilgileri
        orderData.put("userId", user.getUid());
        if (user.getEmail() != null) {
            orderData.put("email", user.getEmail());
        }

        // Teslimat adresi bilgileri
        if (selectedAddress != null) {
            orderData.put("deliveryAddress", createAddressMap(selectedAddress));

            // Teslimat adresi alanlarını da ana seviyeye ekle (OrderDetailActivity için)
            orderData.put("fullName", selectedAddress.getRecipientName() + " " + selectedAddress.getRecipientSurname());
            orderData.put("address", selectedAddress.getAddressLine1());
            orderData.put("city", selectedAddress.getDistrict());
            orderData.put("state", selectedAddress.getProvince());
            orderData.put("phone", selectedAddress.getRecipientPhone());
        }

        // Ödeme bilgileri
        orderData.put("paymentMethod", "Credit Card");
        if (selectedCard != null) {
            Map<String, Object> cardInfo = new HashMap<>();
            cardInfo.put("cardName", selectedCard.getCardName());
            cardInfo.put("maskedCardNumber", selectedCard.getMaskedCardNumber());
            orderData.put("paymentCardInfo", cardInfo);
        }

        // Fiyat bilgileri
        double subtotal = Cart.getInstance().getTotalPrice();
        double shipping = 0.0; // Ücretsiz kargo
        double discount = 5.0; // Kupon indirimi
        double total = subtotal + shipping - discount;

        orderData.put("subtotal", subtotal);
        orderData.put("shipping", shipping);
        orderData.put("discount", discount);
        orderData.put("totalAmount", total);

        // Sipariş durumu ve tarihi
        orderData.put("orderDate", FieldValue.serverTimestamp());
        orderData.put("status", "Sipariş Alındı");

        // Kargo bilgileri
        orderData.put("shippingCompany", "CarCare Express");
        orderData.put("trackingNumber", generateTrackingNumber());

        // Tahmini teslimat tarihi
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 4);
        orderData.put("estimatedDeliveryDate", new Date(calendar.getTimeInMillis()));

        // Sözleşme onayları
        Map<String, Object> contractApprovals = new HashMap<>();
        contractApprovals.put("preInformationApproved", checkboxPreInformation.isChecked());
        contractApprovals.put("distanceSalesApproved", checkboxDistanceSales.isChecked());
        contractApprovals.put("kvkkApproved", checkboxKvkk.isChecked());
        contractApprovals.put("communicationConsent", checkboxCommunication.isChecked());
        contractApprovals.put("approvalDate", FieldValue.serverTimestamp());
        orderData.put("contractApprovals", contractApprovals);

        // Sipariş ürünleri
        List<Map<String, Object>> orderItems = new ArrayList<>();
        for (Product product : Cart.getInstance().getItems()) {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", product.getId());
            item.put("productName", product.getName());
            item.put("price", product.getDiscountPrice() > 0 ? product.getDiscountPrice() : product.getPrice());
            item.put("quantity", product.getQuantity());
            item.put("imageBase64", product.getImageBase64());
            orderItems.add(item);
        }
        orderData.put("items", orderItems);

        // ÖNEMLİ: Siparişi kullanıcının alt koleksiyonuna kaydet
        db.collection("users")
                .document(user.getUid())
                .collection("orders")  // Alt koleksiyon
                .add(orderData)
                .addOnSuccessListener(documentReference -> {
                    String orderId = documentReference.getId();

                    // İsteğe bağlı: Ana orders koleksiyonuna da kaydet (admin paneli için)
                    saveToMainOrdersCollection(orderData, orderId);

                    Cart.getInstance().clearCart(this);
                    progressBar.setVisibility(View.GONE);
                    showOrderCompleteDialog(orderId);
                    Log.d(TAG, "Sipariş başarıyla oluşturuldu: " + orderId);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    buttonPay.setEnabled(true);
                    Log.e(TAG, "Sipariş işlenirken hata", e);
                    Toast.makeText(this, "Sipariş işlenirken hata: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }


    private void saveToMainOrdersCollection(Map<String, Object> orderData, String orderId) {
        // Ana orders koleksiyonuna da aynı verilerle kaydet
        orderData.put("userOrderId", orderId); // Alt koleksiyondaki ID'yi referans olarak ekle

        db.collection("orders")
                .document(orderId) // Aynı ID ile kaydet
                .set(orderData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Sipariş ana koleksiyona da kaydedildi: " + orderId);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Ana koleksiyona kaydetme hatası (kritik değil): " + e.getMessage());
                });
    }

    private Map<String, Object> createAddressMap(AddressModel address) {
        Map<String, Object> addressMap = new HashMap<>();
        addressMap.put("title", address.getTitle());
        addressMap.put("province", address.getProvince());
        addressMap.put("district", address.getDistrict());
        addressMap.put("neighborhood", address.getNeighborhood());
        addressMap.put("addressLine1", address.getAddressLine1());
        addressMap.put("addressLine2", address.getAddressLine2());
        addressMap.put("recipientName", address.getRecipientName());
        addressMap.put("recipientSurname", address.getRecipientSurname());
        addressMap.put("recipientPhone", address.getRecipientPhone());
        return addressMap;
    }

    private String generateTrackingNumber() {
        return "CC" + String.valueOf(System.currentTimeMillis()).substring(5);
    }

    private void showOrderCompleteDialog(String orderId) {
        new AlertDialog.Builder(this)
                .setTitle("Sipariş Tamamlandı")
                .setMessage("Siparişiniz başarıyla alındı. Sipariş ID: " + orderId)
                .setPositiveButton("Alışverişe Devam Et", (dialog, which) -> {
                    Intent intent = new Intent(CheckoutActivity.this, StoreActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finishAffinity();
                })
                .setCancelable(false)
                .show();
    }

    private void showContractDialog(String title, String content) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton("Kapat", null)
                .show();
    }

    private String getPreInformationText() {
        return "ÖN BİLGİLENDİRME KOŞULLARI\n\n" +
                "1. TARAFLAR VE KONU\n" +
                "İşbu Ön Bilgilendirme Formu, 6502 sayılı Tüketicinin Korunması Hakkında Kanun'un 48. maddesi ve Mesafeli Sözleşmeler Yönetmeliği uyarınca hazırlanmıştır.\n\n" +
                "SATICI BİLGİLERİ:\n" +
                "Ünvan: CarCare Otomotiv Ltd. Şti.\n" +
                "Adres: Ankara, Türkiye\n" +
                "Telefon: 0312 XXX XX XX\n" +
                "E-posta: info@carcare.com\n\n" +
                "2. ÜRÜN/HİZMET BİLGİLERİ\n" +
                "Satışa konu ürün/hizmetin temel özelikleri, fiyatı ve ödeme şekli sipariş özeti sayfasında belirtilmiştir.\n\n" +
                "3. CAYMA HAKKI\n" +
                "Tüketici, 14 gün içinde herhangi bir gerekçe göstermeksizin ve cezai şart ödemeksizin sözleşmeden cayabilir.\n\n" +
                "4. TESLİMAT BİLGİLERİ\n" +
                "Ürünler, sipariş tarihinden itibaren en geç 30 gün içinde teslim edilecektir.";
    }

    private String getDistanceSalesText() {
        return "MESAFELİ SATIŞ SÖZLEŞMESİ\n\n" +
                "1. TARAFLAR\n" +
                "SATICI: CarCare Otomotiv Ltd. Şti.\n" +
                "ALICI: Siparişi veren tüketici\n\n" +
                "2. KONU\n" +
                "İşbu sözleşme, ALICI'nın SATICI'ya ait www.carcare.com internet sitesi üzerinden elektronik ortamda siparişini verdiği aşağıda nitelikleri ve satış fiyatı belirtilen ürün/ürünlerin satışı ve teslimi ile ilgili olarak 6502 sayılı Tüketicinin Korunması Hakkında Kanun ve Mesafeli Sözleşmelere Dair Yönetmelik hükümleri gereğince tarafların hak ve yükümlülüklerini kapsar.\n\n" +
                "3. SÖZLEŞME KONUSU ÜRÜN/HİZMET BİLGİLERİ\n" +
                "Ürünlerin temel özellikleri, fiyatları ve ödeme şekli sipariş sayfasında belirtilmiştir.\n\n" +
                "4. GENEL HÜKÜMLER\n" +
                "• Sipariş tarihinde geçerli olan fiyatlar uygulanır.\n" +
                "• Kargo ücreti alıcıya aittir.\n" +
                "• Ürünler hasar görmeyecek şekilde ambalajlanarak teslim edilir.\n\n" +
                "5. CAYMA HAKKI\n" +
                "ALICI, 14 gün içinde herhangi bir hukuki ve cezai sorumluluk üstlenmeksizin sözleşmeden cayabilir.";
    }

    private String getKvkkText() {
        return "KİŞİSEL VERİLERİN KORUNMASI HAKKINDA AYDINLATMA METNİ\n\n" +
                "6698 sayılı Kişisel Verilerin Korunması Kanunu kapsamında, kişisel verileriniz CarCare Otomotiv Ltd. Şti. tarafından aşağıda açıklanan şekilde işlenmektedir.\n\n" +
                "1. VERİ SORUMLUSU\n" +
                "CarCare Otomotiv Ltd. Şti.\n" +
                "Ankara, Türkiye\n\n" +
                "2. KİŞİSEL VERİLERİN İŞLENME AMAÇLARI\n" +
                "• Sipariş ve teslimat işlemlerinin gerçekleştirilmesi\n" +
                "• Müşteri hizmetlerinin sunulması\n" +
                "• Yasal yükümlülüklerin yerine getirilmesi\n" +
                "• İstatistiksel analiz ve raporlama\n\n" +
                "3. TOPLANAN KİŞİSEL VERİLER\n" +
                "• Kimlik bilgileri (ad, soyad)\n" +
                "• İletişim bilgileri (telefon, e-posta, adres)\n" +
                "• Finansal bilgiler (kart bilgileri - güvenli şekilde)\n\n" +
                "4. HAKLARINIZ\n" +
                "• Kişisel verilerinizin işlenip işlenmediğini öğrenme\n" +
                "• İşlenen verileriniz hakkında bilgi talep etme\n" +
                "• Düzeltme ve silme talebinde bulunma\n" +
                "• İtiraz etme hakkı\n\n" +
                "Bu haklarınızı kullanmak için info@carcare.com adresine başvurabilirsiniz.";
    }
}