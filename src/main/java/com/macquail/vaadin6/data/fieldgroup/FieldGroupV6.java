package com.macquail.vaadin6.data.fieldgroup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.macquail.vaadin6.data.transactionalproperty.TransactionalProperty;
import com.macquail.vaadin6.data.transactionalproperty.TransactionalPropertyWrapper;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.ui.Field;

public class FieldGroupV6 implements Serializable {
	private static final Logger logger = Logger.getLogger(FieldGroupV6.class
			.getName());

	private Item itemDataSource;
	private boolean buffered = true;

	private boolean enabled = true;
	private boolean readOnly = false;
	
	private Map<Field, Object> fieldToPropertyId = new HashMap<>();
	private Map<Object, Field> propertyIdToField = new LinkedHashMap<>();
	private List<CommitHandler> commitHandlers = new ArrayList<CommitHandler>();
	
    public FieldGroupV6(Item itemDataSource) {
        setItemDataSource(itemDataSource);
    }
	
    public void setItemDataSource(Item itemDataSource) {
        this.itemDataSource = itemDataSource;

        for (Field f : fieldToPropertyId.keySet()) {
            bind(f, fieldToPropertyId.get(f));
        }
    }
    
    public Item getItemDataSource() {
    	return itemDataSource;
    }
    
    public boolean isBuffered() {
    	return buffered;
    }
    
    public void setBuffered(boolean buffered) {
        if (buffered == this.buffered) {
            return;
        }

        this.buffered = buffered;
        for (Field field : getFields()) {
            setFieldBuffered(field, buffered);
        }
    }

	private void setFieldBuffered(Field field, boolean buffered) {
		field.setReadThrough(!buffered);
		field.setWriteThrough(!buffered);
	}
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean fieldsEnabled) {
        enabled = fieldsEnabled;
        for (Field field : getFields()) {
            field.setEnabled(fieldsEnabled);
        }
    }
    
    public boolean isReadOnly() {
        return readOnly;
    }
    
    public void setReadOnly(boolean fieldsReadOnly) {
        readOnly = fieldsReadOnly;
        for (Field field : getFields()) {
            if (!field.getPropertyDataSource().isReadOnly()) {
                field.setReadOnly(fieldsReadOnly);
            } else {
                field.setReadOnly(true);
            }
        }
    }
    
    public Collection<Field> getFields() {
        return fieldToPropertyId.keySet();
    }
    
    public void bind(Field field, Object propertyId) throws BindException {
        if (propertyIdToField.containsKey(propertyId)
                && propertyIdToField.get(propertyId) != field) {
            throw new BindException("Property id " + propertyId
                    + " is already bound to another field");
        }
        fieldToPropertyId.put(field, propertyId);
        propertyIdToField.put(propertyId, field);
        if (itemDataSource == null) {
            // Will be bound when data source is set
            return;
        }

        field.setPropertyDataSource(wrapInTransactionalProperty(getItemProperty(propertyId)));
        configureField(field);
    }
    
    protected Property getItemProperty(Object propertyId) throws BindException {
        Item item = getItemDataSource();
        if (item == null) {
            throw new BindException("Could not lookup property with id "
                    + propertyId + " as no item has been set");
        }
        Property p = item.getItemProperty(propertyId);
        if (p == null) {
            throw new BindException("A property with id " + propertyId
                    + " was not found in the item");
        }
        return p;
    }
    
    private  TransactionalProperty wrapInTransactionalProperty(
            Property itemProperty) {
        return new TransactionalPropertyWrapper(itemProperty);
    }
    
	/**
	 * CommitHandlers are used by {@link FieldGroup#commit()} as part of the
	 * commit transactions. CommitHandlers can perform custom operations as part
	 * of the commit and cause the commit to be aborted by throwing a
	 * {@link CommitException}.
	 */
	public interface CommitHandler extends Serializable {
		/**
		 * Called before changes are committed to the field and the item is
		 * updated.
		 * <p>
		 * Throw a {@link CommitException} to abort the commit.
		 * 
		 * @param commitEvent
		 *            An event containing information regarding the commit
		 * @throws CommitException
		 *             if the commit should be aborted
		 */
		public void preCommit(CommitEvent commitEvent) throws CommitException;

		/**
		 * Called after changes are committed to the fields and the item is
		 * updated..
		 * <p>
		 * Throw a {@link CommitException} to abort the commit.
		 * 
		 * @param commitEvent
		 *            An event containing information regarding the commit
		 * @throws CommitException
		 *             if the commit should be aborted
		 */
		public void postCommit(CommitEvent commitEvent) throws CommitException;
	}

	public static class CommitEvent implements Serializable {
		private FieldGroupV6 fieldBinder;

		private CommitEvent(FieldGroupV6 fieldBinder) {
			this.fieldBinder = fieldBinder;
		}

		/**
		 * Returns the field binder that this commit relates to
		 * 
		 * @return The FieldBinder that is being committed.
		 */
		public FieldGroupV6 getFieldBinder() {
			return fieldBinder;
		}
	}

	public static class CommitException extends Exception {

		public CommitException() {
			super();
		}

		public CommitException(String message, Throwable cause) {
			super(message, cause);
		}

		public CommitException(String message) {
			super(message);
		}

		public CommitException(Throwable cause) {
			super(cause);
		}

	}
	
    public static class BindException extends RuntimeException {

        public BindException(String message) {
            super(message);
        }

        public BindException(String message, Throwable t) {
            super(message, t);
        }

    }
    
    /**
     * Configures a field with the settings set for this FieldBinder.
     * <p>
     * By default this updates the buffered, read only and enabled state of the
     * field. Also adds validators when applicable. Fields with read only data
     * source are always configured as read only.
     * 
     * @param field
     *            The field to update
     */
    protected void configureField(Field field) {
        setFieldBuffered(field, isBuffered());
        

        field.setEnabled(isEnabled());

        if (field.getPropertyDataSource().isReadOnly()) {
            field.setReadOnly(true);
        } else {
            field.setReadOnly(isReadOnly());
        }
    }
}
