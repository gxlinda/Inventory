package hu.gearxpert.inventory;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import hu.gearxpert.inventory.data.InventoryContract;
import hu.gearxpert.inventory.data.InventoryContract.ProductEntry;
import hu.gearxpert.inventory.data.ProductFileHelper;

public class ProductDetailsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_PRODUCT_LOADER = 0;

    private static String DEFAULT_PRODUCT_PHOTO_URI = "android.resource://hu.gearxpert.inventory/drawable/img_empty_photo";

    static final String STATE_PHOTO_URI_STRING = "photoUriString";

    //Content URI for the existing product (null if it's a new product)
    private Uri mCurrentProductUri;
    private EditText mNameEditText;
    private EditText mDescriptionEditText;
    private EditText mPriceEditText;
    private EditText mCurrencyEditText;
    private EditText mQuantityEditText;
    private Spinner mUnitSpinner;
    private Uri mPhotoUri;
    private String mPhotoUriString;
    private ImageView mPhotoView;
    boolean photoAdded = false;
    String nameString;
    Button addNewSupplier;
    String photoUri;
    Spinner allSuppliersSpinner;
    long selectedSupplierId;
    private String mUnit = ProductEntry.UNIT_PC;
    private static final int PICK_IMAGE_ID = 1;

    //Boolean flag that keeps track of whether the product has been edited (true) or not (false)
    private boolean mProductHasChanged = false;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mProductHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // Setup FAB to open Camera app
        FloatingActionButton fabPhoto = (FloatingActionButton) findViewById(R.id.fab_photo);
        fabPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent chooseImageIntent = ImagePicker.getPickImageIntent(getApplicationContext());
                startActivityForResult(chooseImageIntent, PICK_IMAGE_ID);
            }
        });

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new product or editing an existing one.
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        // If the intent DOES NOT contain a product content URI, then we know that we are
        // creating a new product.
        if (mCurrentProductUri == null) {
            // This is a new product, so change the app bar to say "Add a product"
            setTitle(getString(R.string.title_add_new_product));
            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();
            mPhotoUri = Uri.parse(DEFAULT_PRODUCT_PHOTO_URI);

        } else {
            // Otherwise this is an existing product, so change app bar to say "Edit product"
            setTitle(getString(R.string.title_edit_product));

            //set product image from internal storage
            File productImageFile = ProductFileHelper.getProductImageFile(this, mCurrentProductUri);
            if (productImageFile != null) {
                mPhotoView.setImageURI(Uri.fromFile(productImageFile));
            }

            // Initialize a loader to read the product data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_name);
        mDescriptionEditText = (EditText) findViewById(R.id.edit_description);
        mPriceEditText = (EditText) findViewById(R.id.edit_price);
        mCurrencyEditText = (EditText) findViewById(R.id.edit_currency);
        mQuantityEditText = (EditText) findViewById(R.id.edit_quantity);
        mUnitSpinner = (Spinner) findViewById(R.id.spinner_unit);
        mPhotoView = (ImageView) findViewById(R.id.item_photo);
        addNewSupplier = (Button) findViewById(R.id.add_new_supplier);
        allSuppliersSpinner = (Spinner) findViewById(R.id.supplier_spinner);

        addNewSupplier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent supplier = new Intent(ProductDetailsActivity.this, SupplierDetailsActivity.class);
                startActivity(supplier);
            }
        });

        //prepare  data for spinner
        prepareData();

        //handle click of spinner item
        allSuppliersSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSupplierId = id; //stores the selected item's id >> we need it at saving the product

                if (selectedSupplierId > 0) {
                    Cursor supplier = getSupplierCursor(selectedSupplierId);
                    setViewSupplierData(supplier);
                    supplier.close();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mNameEditText.setOnTouchListener(mTouchListener);
        mDescriptionEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mCurrencyEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mUnitSpinner.setOnTouchListener(mTouchListener);
        mPhotoView.setOnTouchListener(mTouchListener);

        setupUnitSpinner();

        //set order button disabled as default
        Button button = (Button) findViewById(R.id.order_button);
        button.setEnabled(false);
    }

    private void setupUnitSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter unitSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_unit_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        unitSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mUnitSpinner.setAdapter(unitSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mUnitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.unit_pc))) {
                        mUnit = ProductEntry.UNIT_PC;
                    } else if (selection.equals(getString(R.string.unit_kg))) {
                        mUnit = ProductEntry.UNIT_KG;
                    } else {
                        mUnit = ProductEntry.UNIT_L;
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mUnit = ProductEntry.UNIT_PC;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_IMAGE_ID:
                Bitmap bitmap = ImagePicker.getImageFromResult(this, resultCode, data);
                File tempImageFile = ProductFileHelper.putProductTemporaryImageFile(this, bitmap);
                mPhotoView.setImageURI(Uri.fromFile(tempImageFile));
                mPhotoUri = Uri.fromFile(tempImageFile);
                mPhotoUriString = mPhotoUri.toString();
                photoAdded = true;
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    String priceString;
    String currencyString;
    String quantityString;

    /**
     * Get user input from editor and save product into database.
     */
    private void saveProduct() {

        // Read from input field
        // Use trim to eliminate leading or trailing white space
        String descriptionString = mDescriptionEditText.getText().toString().trim();

        // Create a ContentValues object where column names are the keys,
        // and product attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        values.put(ProductEntry.COLUMN_PRODUCT_DESCRIPTION, descriptionString);
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY_ON_STOCK, quantityString);
        values.put(ProductEntry.COLUMN_PRODUCT_UNIT, mUnit);
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER_ID, selectedSupplierId);

        // If the price is not provided by the user, don't try to parse the string into an
        // integer value. Use 0 by default.
        int price = 0;
        if (!TextUtils.isEmpty(priceString)) {
            price = Integer.parseInt(priceString);
        }
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);

        String currency = "HUF";
        if (!TextUtils.isEmpty(currencyString)) {
            currency = currencyString;
        }
        values.put(ProductEntry.COLUMN_PRODUCT_CURRENCY, currency);

        if (mPhotoUri == null) {
            mPhotoUri = Uri.parse(DEFAULT_PRODUCT_PHOTO_URI);
        }
        values.put(ProductEntry.COLUMN_PRODUCT_PHOTO, mPhotoUri.toString());

        // Determine if this is a new or existing product by checking if mCurrentProductUri is null or not
        if (mCurrentProductUri == null) {
            // This is a NEW product, so insert a new product into the provider,
            // returning the content URI for the new product.
            Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an EXISTING product, so update the product with content URI: mCurrentProductUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentProductUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * method to save instance state when activity is about to be brought down.
     * most of the hard work about EditText fields etc. is done by Android itself, so we only
     * need to take care about temporary image state.
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString(STATE_PHOTO_URI_STRING, photoUri);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mPhotoUriString = savedInstanceState.getString(STATE_PHOTO_URI_STRING);
        mPhotoView.setImageURI(Uri.parse(mPhotoUriString));
    }

    public void onResume() {
        super.onResume();
        mPhotoView.invalidate();
        prepareData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_product_editor, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item.
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    String validationMessage;

    public boolean dataValidation() {

        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        nameString = mNameEditText.getText().toString().trim();
        priceString = mPriceEditText.getText().toString().trim();
        currencyString = mCurrencyEditText.getText().toString().trim();
        quantityString = mQuantityEditText.getText().toString().trim();

        validationMessage = "";
        boolean dataAreValid = true;
        if (TextUtils.isEmpty(nameString)) {
            dataAreValid = false;
            validationMessage = getString(R.string.valid_enter_name_product);
        }

        if (!isIntValid(priceString)) {
            dataAreValid = false;
            validationMessage += getString(R.string.valid_price_invalid);
        }

        if (!mUnit.equals("pc")
                && !mUnit.equals("kg")
                && !mUnit.equals("l")
                ) {
            dataAreValid = false;
            validationMessage += getString(R.string.valid_unit_invalid);
        }

        if (!isIntValid(quantityString)) {
            dataAreValid = false;
            validationMessage += getString(R.string.valid_quantity_invalid);
        }
        return dataAreValid;
    }

    /**
     * Checks the text as int. It is true if value can be handled as valid number
     *
     * @param value
     * @return
     */
    private boolean isIntValid(String value) {
        boolean _ret = true;

        if (value == null) {
            _ret = false;
        }
        if (TextUtils.isEmpty(value)) {
            _ret = false;
        }
        if (_ret) {
            int _val = Integer.parseInt(value);
            if (_val < 0) {
                _ret = false;
            }
        }

        return _ret;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean _ret = false;

        //region *VALIDATION

        boolean _valid = true;

        if (item.getItemId() == R.id.action_save) {     //we validate only at Save menu option
            _valid = dataValidation();
        }

        if (!_valid)        //if it is invalid, a Toast is shown
        {
            Toast.makeText(this, validationMessage, Toast.LENGTH_SHORT).show();
            validationMessage = "";
            return false;
        }
        //endregion

        //region *MENU OPTIONS
        if (_valid) {
            // User clicked on a menu option in the app bar overflow menu
            switch (item.getItemId()) {
                case R.id.action_save: // Respond to a click on the "Save" menu option
                    // Save product to database
                    saveProduct();
                    // Exit activity
                    finish();
                    _ret = true;
                    break;
                case R.id.action_delete: // Respond to a click on the "Delete" menu option
                    // Pop up confirmation dialog for deletion
                    showDeleteConfirmationDialog();
                    _ret = true;
                    break;
                case android.R.id.home: // Respond to a click on the "Up" arrow button in the app bar
                    // If the product hasn't changed, continue with navigating up to parent activity
                    // which is the {@link CatalogActivity}.
                    if (!mProductHasChanged) {
                        NavUtils.navigateUpFromSameTask(ProductDetailsActivity.this);
                        _ret = true;
                    }

                    // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                    // Create a click listener to handle the user confirming that
                    // changes should be discarded.
                    if (mProductHasChanged) {
                        DialogInterface.OnClickListener discardButtonClickListener =
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        // User clicked "Discard" button, navigate to parent activity.
                                        NavUtils.navigateUpFromSameTask(ProductDetailsActivity.this);
                                    }
                                };

                        // Show a dialog that notifies the user they have unsaved changes
                        showUnsavedChangesDialog(discardButtonClickListener);
                    }
                    _ret = true;
                    break;
            }
        }
        //endregion

        return _ret;
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the product in the database.
     */
    private void deleteProduct() {
        // Only perform the delete if this is an existing product.
        if (mCurrentProductUri != null) {
            // Call the ContentResolver to delete the product at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentProductUri
            // content URI already identifies the product that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                ProductFileHelper.deleteProductImage(this, mCurrentProductUri);
                Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        // Close the activity
        finish();
    }

    public void increaseQuantity(View v) {
        int quantity = Integer.parseInt(mQuantityEditText.getText().toString().trim());
        mQuantityEditText.setText(String.valueOf(quantity + 1));
    }

    public void decreaseQuantity(View v) {
        int quantity = Integer.parseInt(mQuantityEditText.getText().toString().trim());
        if (quantity > 0) {
            mQuantityEditText.setText(String.valueOf(quantity - 1));
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all product attributes, define a projection that contains
        // all columns from the product table
        String[] projection = {
                ProductEntry.TABLE_NAME_PRODUCTS + "." + ProductEntry.PRODUCT_ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_DESCRIPTION,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_CURRENCY,
                ProductEntry.COLUMN_PRODUCT_QUANTITY_ON_STOCK,
                ProductEntry.COLUMN_PRODUCT_UNIT,
                ProductEntry.COLUMN_PRODUCT_PHOTO,
                ProductEntry.TABLE_NAME_PRODUCTS + "." + ProductEntry.COLUMN_PRODUCT_SUPPLIER_ID,
                InventoryContract.SupplierEntry.TABLE_NAME_SUPPLIERS + "." + InventoryContract.SupplierEntry.COLUMN_SUPPLIER_NAME,
                InventoryContract.SupplierEntry.TABLE_NAME_SUPPLIERS + "." + InventoryContract.SupplierEntry.COLUMN_SUPPLIER_ADDRESS,
                InventoryContract.SupplierEntry.TABLE_NAME_SUPPLIERS + "." + InventoryContract.SupplierEntry.COLUMN_SUPPLIER_PHONE,
                InventoryContract.SupplierEntry.TABLE_NAME_SUPPLIERS + "." + InventoryContract.SupplierEntry.COLUMN_SUPPLIER_EMAIL
        };

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentProductUri,         // Query the content URI for the current product
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of product attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int descriptionColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_DESCRIPTION);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int currencyColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_CURRENCY);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY_ON_STOCK);
            int unitColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_UNIT);
            int photoColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PHOTO);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            String description = cursor.getString(descriptionColumnIndex);
            int price = cursor.getInt(priceColumnIndex);
            String currency = cursor.getString(currencyColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            String unit = cursor.getString(unitColumnIndex);
            photoUri = cursor.getString(photoColumnIndex);

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mDescriptionEditText.setText(description);
            mPriceEditText.setText(Integer.toString(price));
            mCurrencyEditText.setText(currency);
            mQuantityEditText.setText(Integer.toString(quantity));
            mPhotoView.setImageURI(Uri.parse(photoUri));

            // Unit is a dropdown spinner, so map the constant value from the database
            // into one of the dropdown options (0 is pc, 1 is kg, 2 is l).
            // Then call setSelection() so that option is displayed on screen as the current selection.
            switch (unit) {
                case ProductEntry.UNIT_KG:
                    mUnitSpinner.setSelection(1);
                    break;
                case ProductEntry.UNIT_L:
                    mUnitSpinner.setSelection(2);
                    break;
                default:
                    mUnitSpinner.setSelection(0);
                    break;
            }
        }
        //sets spinner to display the actual supplier from the db
        selectedSupplierId = cursor.getInt(
                cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SUPPLIER_ID)
        );

        Button button = (Button) findViewById(R.id.order_button);
        if (selectedSupplierId > 0) {
            //if there is saved data for supplier in the db, we enable the order button
            setSpinnerItemById(allSuppliersSpinner, selectedSupplierId);
            setViewSupplierData(cursor);
            button.setEnabled(true);
        } else {//if there is no supplier at the db, we disable the order button
            button.setEnabled(false);
        }

    }

    //display data for supplier in the views
    private void setViewSupplierData(Cursor pCursor) {

        TextView view;

        //address
        view = (TextView) findViewById(R.id.p_supplier_address);
        int _columnindex = pCursor.getColumnIndex(InventoryContract.SupplierEntry.COLUMN_SUPPLIER_ADDRESS);
        String _value = pCursor.getString(_columnindex);
        view.setText(_value);
        view = null;

        //phone
        view = (TextView) findViewById(R.id.p_supplier_phone);
        view.setText(pCursor.getString(
                pCursor.getColumnIndex(InventoryContract.SupplierEntry.COLUMN_SUPPLIER_PHONE)
                )
        );
        view = null;

        //email
        view = (TextView) findViewById(R.id.p_supplier_email);
        view.setText(pCursor.getString(
                pCursor.getColumnIndex(InventoryContract.SupplierEntry.COLUMN_SUPPLIER_EMAIL)
                )
        );
        view = null;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mDescriptionEditText.setText("");
        mPriceEditText.setText("");
        mCurrencyEditText.setText("HUF");
        mQuantityEditText.setText("");
        mUnitSpinner.setSelection(0); // Select pc as unit
        mPhotoView.setImageURI(Uri.parse(DEFAULT_PRODUCT_PHOTO_URI));
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    //open Intent to order from supplier by email
    public void orderFromSupplier(View view) {
        //gets selected supplier's data
        Cursor supplier = getSupplierCursor(selectedSupplierId);

        int _columnindex = supplier.getColumnIndex(InventoryContract.SupplierEntry.COLUMN_SUPPLIER_EMAIL);
        String _email = supplier.getString(_columnindex);
        _columnindex = supplier.getColumnIndex(InventoryContract.SupplierEntry.COLUMN_SUPPLIER_NAME);
        String _supplierName = supplier.getString(_columnindex);
        String _productName = mNameEditText.getText().toString();

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", _email, null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject));
        emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_title1) + _supplierName + getString(R.string.email_title_mark) + "\n\n"
                + getString(R.string.email_body) + "\n\n" + _productName);
        startActivity(Intent.createChooser(emailIntent, getString(R.string.email_send_by)));
    }

    public void prepareData() {
        // Columns from db to map into the view file
        String[] fromColumns = {
                InventoryContract.SupplierEntry.COLUMN_SUPPLIER_NAME,
                InventoryContract.SupplierEntry.SUPPLIER_ID

        };
        // View IDs to map the columns (fetched above) into
        int[] toViews = {
                android.R.id.text1
        };

        Cursor suppliers = getContentResolver().query(
                InventoryContract.SupplierEntry.CONTENT_URI,
                null,
                null,
                null,
                InventoryContract.SupplierEntry.COLUMN_SUPPLIER_NAME + " ASC"
        );

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this, // context
                android.R.layout.simple_spinner_item, // layout file
                suppliers, // DB cursor
                fromColumns, // data to bind to the UI
                toViews, // views that'll represent the data from `fromColumns`
                0
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Create the list view and bind the adapter
        allSuppliersSpinner.setAdapter(adapter);

        setSpinnerItemById(allSuppliersSpinner, InventoryUtils.Vars.NewSupplierId);

        selectedSupplierId = InventoryUtils.Vars.NewSupplierId;

        //show selected supplier's data
        if (selectedSupplierId > 0) {
            Cursor supplier = getSupplierCursor(selectedSupplierId);
            setViewSupplierData(supplier);
            supplier.close();
        }
    }

    /**
     * gets selected supplier's data in a cursor
     *
     * @param pSelectedSupplierId
     * @return
     */
    private Cursor getSupplierCursor(long pSelectedSupplierId) {
        String selection = InventoryContract.SupplierEntry.SUPPLIER_ID + "=?";
        String[] selectionArgs = {String.valueOf(pSelectedSupplierId)};

        Cursor supplier = getContentResolver().query(
                InventoryContract.SupplierEntry.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null
        );
        supplier.moveToFirst();
        return supplier;
    }

    //sets the spinner value by pId
    public void setSpinnerItemById(Spinner spinner, long pId) {
        int spinnerCount = spinner.getCount();
        for (int i = 0; i < spinnerCount; i++) {
            long _id = spinner.getItemIdAtPosition(i);
            if (_id == pId) {
                spinner.setSelection(i);
            }
        }
    }
}
