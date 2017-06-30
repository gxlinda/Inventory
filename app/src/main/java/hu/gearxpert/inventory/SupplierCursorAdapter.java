package hu.gearxpert.inventory;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import static hu.gearxpert.inventory.data.InventoryContract.SupplierEntry;

/**
 * Created by melinda.kostenszki on 2017.06.26..
 */

public class SupplierCursorAdapter extends CursorAdapter {

    public SupplierCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    //Makes a new blank list item view. No data is set (or bound) to the views yet.
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item_supplier, parent, false);
    }

    //This method binds the supplier data (in the current row pointed to by cursor) to the given list item layout.
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        TextView tvSupplierName = (TextView) view.findViewById(R.id.supplier_name);
        TextView tvSupplierAddress = (TextView) view.findViewById(R.id.supplier_address);
        TextView tvSupplierPhone = (TextView) view.findViewById(R.id.supplier_phone);
        TextView tvSupplierEmail = (TextView) view.findViewById(R.id.supplier_email);

        final int SupplierId = cursor.getInt(cursor.getColumnIndexOrThrow(SupplierEntry.SUPPLIER_ID));
        String supplierName = cursor.getString(cursor.getColumnIndexOrThrow(SupplierEntry.COLUMN_SUPPLIER_NAME));
        String supplierAddress = cursor.getString(cursor.getColumnIndexOrThrow(SupplierEntry.COLUMN_SUPPLIER_ADDRESS));
        String supplierPhone = cursor.getString(cursor.getColumnIndexOrThrow(SupplierEntry.COLUMN_SUPPLIER_PHONE));
        String supplierEmail = cursor.getString(cursor.getColumnIndexOrThrow(SupplierEntry.COLUMN_SUPPLIER_EMAIL));

        tvSupplierName.setText(supplierName);
        tvSupplierAddress.setText(supplierAddress);
        tvSupplierPhone.setText(supplierPhone);
        tvSupplierEmail.setText(supplierEmail);
    }
}
