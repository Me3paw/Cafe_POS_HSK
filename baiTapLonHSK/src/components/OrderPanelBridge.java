package components;

/**
 * Bridge interface that allows external containers to configure the generic
 * order-entry panel (CapNhat.OrderPanel) for special flows such as appending
 * món to an existing đơn hàng.
 */
public interface OrderPanelBridge {

    /**
     * Switch the panel into "append to existing order" mode.
     *
     * @param maDonHang  the target order id
     * @param onSuccess  callback invoked when items are successfully appended
     */
    void configureForExistingOrder(int maDonHang, Runnable onSuccess);
}
