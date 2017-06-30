package hu.gearxpert.inventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import hu.gearxpert.inventory.data.InventoryContract.ProductEntry;

/**
 * Created by melinda.kostenszki on 2017.06.20..
 */

public class ProductCursorAdapter extends CursorAdapter {

    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    //Makes a new blank list item view. No data is set (or bound) to the views yet.
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item_product, parent, false);
    }

    //This method binds the products data (in the current row pointed to by cursor) to the given list item layout.
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        TextView tvProductName = (TextView) view.findViewById(R.id.name);
        TextView tvProductDescription = (TextView) view.findViewById(R.id.description);
        TextView tvProductPrice = (TextView) view.findViewById(R.id.price);
        TextView tvProductQuantity = (TextView) view.findViewById(R.id.quantity);
        TextView tvProductCurrency = (TextView) view.findViewById(R.id.currency);
        TextView tvProductUnit = (TextView) view.findViewById(R.id.unit);
        Button saleButton = (Button) view.findViewById(R.id.sale_button);

        final int productId = cursor.getInt(cursor.getColumnIndexOrThrow(ProductEntry.PRODUCT_ID));
        String productName = cursor.getString(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_NAME));
        String productDescription = cursor.getString(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_DESCRIPTION));
        double productPrice = cursor.getDouble(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_PRICE));
        final int productQuantity = cursor.getInt(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_QUANTITY_ON_STOCK));
        String productCurrency = cursor.getString(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_CURRENCY));
        String productUnit = cursor.getString(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_UNIT));

        // If the product description is empty string or null, then use some default text
        if (TextUtils.isEmpty(productDescription)) {
            productDescription = context.getString(R.string.no_description);
        }

        if (productQuantity < 1) {
            //if product is out of stock, sets views and sale button accordingly
            saleButton.setVisibility(View.GONE);
            tvProductQuantity.setText("0");
        } else {
            //if product is on stock, sets views and sale button accordingly
            saleButton.setVisibility(View.VISIBLE);
            tvProductQuantity.setText(String.valueOf(productQuantity));
            saleButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sellProduct(context, productId, productQuantity);
                }
            });
        }

        tvProductName.setText(productName);
        tvProductDescription.setText(productDescription);
        tvProductPrice.setText(String.valueOf(productPrice));
        tvProductCurrency.setText(productCurrency);
        tvProductUnit.setText(productUnit);
    }

    private boolean sellProduct(Context context, long productId, int quantityAvailable) {

        Resources resources = context.getResources();

        Uri productUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, productId);
        ContentValues updateData = new ContentValues();
        updateData.put(ProductEntry.COLUMN_PRODUCT_QUANTITY_ON_STOCK, quantityAvailable - 1);

        //try to save quantity decreased by one
        boolean result = false;
        String toastMessage;
        try {
            int rowsAffected = context.getContentResolver().update(productUri, updateData, null, null);
            if (rowsAffected > 0) {
                toastMessage = resources.getString(R.string.product_sold_successfully);
                result = true;
            } else {
                toastMessage = resources.getString(R.string.db_update_failed);
            }
        } catch (IllegalArgumentException e) {
            toastMessage = e.getMessage();
        }

        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();

        return result;
    }
}