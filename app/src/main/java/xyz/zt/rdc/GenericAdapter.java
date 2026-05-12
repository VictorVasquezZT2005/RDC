package xyz.zt.rdc;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.button.MaterialButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class GenericAdapter extends RecyclerView.Adapter<GenericAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onEdit(DocumentSnapshot doc);
        void onDelete(DocumentSnapshot doc);
        default void onToggleStatus(DocumentSnapshot doc, boolean isDisabled) {}
        default void onItemClick(DocumentSnapshot doc) {}
    }

    private List<DocumentSnapshot> list;
    private OnItemClickListener listener;
    private String titleField, subtitleField;
    private String restrictedEmail = null;
    private boolean showConfig = false;
    private boolean isReadOnly = false;

    public GenericAdapter(List<DocumentSnapshot> list, String titleField, String subtitleField, OnItemClickListener listener) {
        this.list = list;
        this.titleField = titleField;
        this.subtitleField = subtitleField;
        this.listener = listener;
    }

    public void setReadOnly(boolean readOnly) {
        this.isReadOnly = readOnly;
    }

    public void setShowConfig(boolean showConfig) {
        this.showConfig = showConfig;
    }

    public void setRestrictedEmail(String email) {
        this.restrictedEmail = email;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = list.get(position);
        String title = doc.getString(titleField);
        String subtitle = doc.getString(subtitleField);
        
        // Set leading icon based on context
        if (titleField.equals("name") && subtitleField.equals("code")) {
            holder.ivIcon.setImageResource(R.drawable.school);
        } else if (titleField.equals("name") && subtitleField.equals("role")) {
            holder.ivIcon.setImageResource(R.drawable.account_circle);
        } else if (titleField.equals("studentName")) {
            holder.ivIcon.setImageResource(R.drawable.problem);
        }
        
        Boolean isDisabled = doc.getBoolean("disabled");
        boolean currentlyDisabled = isDisabled != null && isDisabled;

        if (currentlyDisabled) {
            holder.tvTitle.setText(title);
            holder.tvTitle.setAlpha(0.6f);
            holder.tvSubtitle.setAlpha(0.6f);
            holder.ivIcon.setAlpha(0.6f);
            holder.btnConfig.setIconResource(R.drawable.pill_off);
            holder.btnConfig.setIconTintResource(R.color.brand_tertiary);
        } else {
            holder.tvTitle.setText(title);
            holder.tvTitle.setAlpha(1.0f);
            holder.tvSubtitle.setAlpha(1.0f);
            holder.ivIcon.setAlpha(1.0f);
            holder.btnConfig.setIconResource(R.drawable.pill);
            holder.btnConfig.setIconTintResource(R.color.brand_primary);
        }
        
        holder.tvSubtitle.setText(subtitle);
        
        // Ensure no text is shown for icon-only buttons
        holder.btnEdit.setText("");
        holder.btnDelete.setText("");
        holder.btnConfig.setText("");

        // Click listener for the entire item
        holder.itemView.setOnClickListener(v -> listener.onItemClick(doc));

        // Toggle Status Button (Repurposed btnConfig)
        if (!isReadOnly && (showConfig || titleField.equals("name") && subtitleField.equals("role"))) {
            holder.btnConfig.setVisibility(View.VISIBLE);
            holder.btnConfig.setOnClickListener(v -> {
                String action = currentlyDisabled ? "habilitar" : "deshabilitar";
                new MaterialAlertDialogBuilder(v.getContext())
                        .setTitle("¿" + action.substring(0, 1).toUpperCase() + action.substring(1) + " usuario?")
                        .setMessage("¿Estás seguro de que deseas " + action + " a este usuario?")
                        .setIcon(R.drawable.warning)
                        .setPositiveButton(currentlyDisabled ? "Habilitar" : "Deshabilitar", (dialog, which) -> {
                            listener.onToggleStatus(doc, currentlyDisabled);
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
        } else {
            holder.btnConfig.setVisibility(View.GONE);
        }

        String email = doc.getString("email");
        boolean isRestricted = restrictedEmail != null && restrictedEmail.equalsIgnoreCase(email);

        if (isRestricted || isReadOnly) {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
            holder.btnConfig.setVisibility(View.GONE);
        } else {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
            
            holder.btnEdit.setOnClickListener(v -> listener.onEdit(doc));
            
            holder.btnDelete.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(v.getContext())
                        .setTitle("¿Eliminar registro?")
                        .setMessage("Esta acción no se puede deshacer y borrará permanentemente los datos. ¿Estás seguro?")
                        .setIcon(R.drawable.warning)
                        .setPositiveButton("Eliminar", (dialog, which) -> listener.onDelete(doc))
                        .setNegativeButton("Cancelar", null)
                        .show();
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle;
        MaterialButton btnEdit, btnDelete, btnConfig;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvSubtitle = itemView.findViewById(R.id.tvItemSubtitle);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnConfig = itemView.findViewById(R.id.btnConfig);
            ivIcon = itemView.findViewById(R.id.ivItemIcon);
        }
    }
}