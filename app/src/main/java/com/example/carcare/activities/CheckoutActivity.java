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

import com.example.carcare.models.CartItem;
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
            Toast.makeText(this, "Your cart is empty. You are being redirected to the store.", Toast.LENGTH_LONG).show();
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
        // Adapter'a context'i ve CartItem listesini gönderiyoruz.
        orderAdapter = new OrderSummaryAdapter(Cart.getInstance().getItems(), this);
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
        textViewPreInformation.setOnClickListener(v -> showContractDialog("Preliminary Information Conditions", getPreInformationText()));
        textViewDistanceSales.setOnClickListener(v -> showContractDialog("Distance Sales Contract", getDistanceSalesText()));
        textViewKvkk.setOnClickListener(v -> showContractDialog("KVKK Information Text", getKvkkText()));

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
            textShipping.setText("Free");
            textShipping.setTextColor(ContextCompat.getColor(this, R.color.green_success));
        }

        textTotal.setText(tlFormat.format(total) + " TL");

        // MaterialButton içindeki fiyat güncellemesi
        MaterialButton buttonPay = findViewById(R.id.buttonPay);
        buttonPay.setText("Confirm and Finish • " + tlFormat.format(total) + " TL");

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
            textAddEditAddress.setText("Change");
        } else {
            showNoAddressState();
        }
    }

    private void showNoAddressState() {
        textSelectedAddressTitle.setText("Select shipping address");
        textSelectedAddressLine1.setText("");
        textSelectedAddressLine2.setText("");
        textSelectedRecipientInfo.setText("");
        textAddEditAddress.setText("Add");
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
                    Log.e(TAG, "Error loading selected address", e);
                    Toast.makeText(this, "An error occurred while loading the address.", Toast.LENGTH_SHORT).show();
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
                    Log.e(TAG, "Error loading default card", e);
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
                    Log.e(TAG, "Error loading card", e);
                    showNoCardState();
                });
    }

    private void updateSelectedCardUI() {
        if (selectedCard != null) {
            textSelectedCardName.setText(selectedCard.getCardName());
            textSelectedCardNumber.setText(selectedCard.getMaskedCardNumber());
            textSelectedCardHolder.setText(selectedCard.getCardHolderName());
            textAddEditCard.setText("Pay with Another Card");
        } else {
            showNoCardState();
        }
    }

    private void showNoCardState() {
        textSelectedCardName.setText("Select card information");
        textSelectedCardNumber.setText("");
        textSelectedCardHolder.setText("");
        textAddEditCard.setText("Add");
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
                    Log.e(TAG, "Error loading selected card", e);
                    Toast.makeText(this, "An error occurred while loading the card", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Please select a shipping address.", Toast.LENGTH_SHORT).show();
            layoutDeliveryAddress.requestFocus();
            return false;
        }

        if (selectedCard == null) {
            Toast.makeText(this, "Please select a payment card.", Toast.LENGTH_SHORT).show();
            layoutCardSelection.requestFocus();
            return false;
        }

        if (Cart.getInstance().getItems().isEmpty()) {
            Toast.makeText(this, "Your cart is empty! You are being redirected to the store.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, StoreActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return false;
        }

        if (!checkboxPreInformation.isChecked()) {
            Toast.makeText(this, "Please confirm the Preliminary Information Conditions.", Toast.LENGTH_SHORT).show();
            checkboxPreInformation.requestFocus();
            return false;
        }

        if (!checkboxDistanceSales.isChecked()) {
            Toast.makeText(this, "Please confirm the Distance Selling Agreement.", Toast.LENGTH_SHORT).show();
            checkboxDistanceSales.requestFocus();
            return false;
        }

        if (!checkboxKvkk.isChecked()) {
            Toast.makeText(this, "Please approve the KVKK Information Text.", Toast.LENGTH_SHORT).show();
            checkboxKvkk.requestFocus();
            return false;
        }
        return true;
    }

    private void processOrder() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_LONG).show();
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
        orderData.put("status", "Order Received");

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
        // for (Product product : Cart.getInstance().getItems()) { // Eski hali
        for (CartItem cartItem : Cart.getInstance().getItems()) { // Yeni hali
            Product product = cartItem.getProduct();
            Map<String, Object> item = new HashMap<>();
            item.put("productId", product.getId());
            item.put("productName", product.getName());
            item.put("price", product.getFinalPrice()); // getFinalPrice() kullanmak daha güvenli
            item.put("quantity", cartItem.getQuantity()); // Miktarı cartItem'dan al
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
                    Log.d(TAG, "Order created successfully: " + orderId);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    buttonPay.setEnabled(true);
                    Log.e(TAG, "Error while processing order", e);
                    Toast.makeText(this, "Error while processing order: " + e.getMessage(),
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
                    Log.d(TAG, "The order was also recorded in the main collection: " + orderId);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error saving to main collection (non-critical): " + e.getMessage());
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
                .setTitle("Order Completed")
                .setMessage("Your order has been successfully placed. Order ID: " + orderId)
                .setPositiveButton("Continue Shopping", (dialog, which) -> {
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
        return "PRELIMINARY INFORMATION CONDITIONS\n\n" +
                "1. PARTIES AND SUBJECT\n" +
                "This Preliminary Information Form has been prepared in accordance with Article 48 of the Consumer Protection Law No. 6502 and the Distance Contracts Regulation.\n\n" +
                "SELLER INFORMATION:\n" +
                "Title: CarCare Automotive Ltd. Sti.\n" +
                "Address: Ankara, Türkiye\n" +
                "Phone: 0312 XXX XX XX\n" +
                "E-mail: info@carcare.com\n\n" +
                "2. PRODUCT/SERVICE INFORMATION\n" +
                "The main features, price, and payment method of the product/service subject to sale are specified on the order summary page.\n\n" +
                "3. RIGHT OF WITHDRAWAL\n" +
                "The consumer can withdraw from the contract within 14 days without providing any justification and without paying any penalty.\n\n" +
                "4. DELIVERY INFORMATION\n" +
                "The products will be delivered no later than 30 days from the order date.";
    }

    private String getDistanceSalesText() {
        return "DISTANCE SALES AGREEMENT\n\n" +
                "1. PARTIES\n" +
                "SELLER: CarCare Automotive Ltd. Sti.\n" +
                "BUYER: The consumer placing the order\n\n" +
                "2. SUBJECT\n" +
                "This agreement covers the rights and obligations of the parties regarding the sale and delivery of the product(s) whose features and sales price are specified below, which the BUYER ordered electronically from the SELLER's website www.carcare.com, in accordance with the provisions of the Law on the Protection of the Consumer No. 6502 and the Regulation on Distance Contracts.\n\n" +
                "3. PRODUCT/SERVICE INFORMATION SUBJECT TO THE CONTRACT\n" +
                "The main features, prices, and payment method of the products are specified on the order page.\n\n" +
                "4. GENERAL PROVISIONS\n" +
                "• The prices valid at the time of the order will be applied.\n" +
                "• The shipping fee is the responsibility of the buyer.\n" +
                "• The products are packaged and delivered in a way that prevents damage.\n\n" +
                "5. RIGHT OF WITHDRAWAL\n" +
                "The BUYER can withdraw from the contract within 14 days without undertaking any legal or penal liability.";
    }

    private String getKvkkText() {
        return "CLARIFICATION TEXT ON THE PROTECTION OF PERSONAL DATA\n\n" +
                "Within the scope of the Law on the Protection of Personal Data No. 6698, your personal data is processed by CarCare Automotive Ltd. Sti. as described below.\n\n" +
                "1. DATA CONTROLLER\n" +
                "CarCare Automotive Ltd. Sti.\n" +
                "Ankara, Türkiye\n\n" +
                "2. PURPOSES OF PROCESSING PERSONAL DATA\n" +
                "• To carry out order and delivery processes\n" +
                "• To provide customer services\n" +
                "• To fulfill legal obligations\n" +
                "• Statistical analysis and reporting\n\n" +
                "3. PERSONAL DATA COLLECTED\n" +
                "• Identity information (name, surname)\n" +
                "• Contact information (phone, e-mail, address)\n" +
                "• Financial information (card details - securely)\n\n" +
                "4. YOUR RIGHTS\n" +
                "• To learn whether your personal data is being processed\n" +
                "• To request information about your processed data\n" +
                "• To request correction and deletion\n" +
                "• Right to object\n\n" +
                "To exercise these rights, you can contact us at info@carcare.com.";
    }
}
