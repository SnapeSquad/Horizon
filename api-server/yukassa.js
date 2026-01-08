/**
 * ЮKassa Integration Module
 * Для работы необходимо указать shopId и secretKey в .env
 */

const { YooCheckout } = require('@a2seven/yoo-checkout');

// ЮKassa Configuration
const YUKASSA_SHOP_ID = process.env.YUKASSA_SHOP_ID || 'ВАDIFF_SHOP_ID'; // Заменить на реальный
const YUKASSA_SECRET_KEY = process.env.YUKASSA_SECRET_KEY || 'ВАШ_SECRET_KEY'; // Заменить на реальный

const checkout = new YooCheckout({
    shopId: YUKASSA_SHOP_ID,
    secretKey: YUKASSA_SECRET_KEY
});

/**
 * Создать платеж
 * @param {number} amount - Сумма в рублях
 * @param {string} description - Описание платежа
 * @param {string} username - Никнейм пользователя
 * @param {string} returnUrl - URL возврата после оплаты
 */
async function createPayment(amount, description, username, returnUrl) {
    try {
        const idempotenceKey = `${Date.now()}_${username}_${Math.random().toString(36)}`;
        
        const payment = await checkout.createPayment({
            amount: {
                value: amount.toFixed(2),
                currency: 'RUB'
            },
            confirmation: {
                type: 'redirect',
                return_url: returnUrl
            },
            capture: true,
            description: description,
            metadata: {
                username: username,
                order_id: `order_${Date.now()}`
            }
        }, idempotenceKey);

        return {
            success: true,
            paymentId: payment.id,
            confirmationUrl: payment.confirmation.confirmation_url,
            status: payment.status
        };
    } catch (error) {
        console.error('[YUKASSA] Create payment error:', error);
        return {
            success: false,
            message: error.message
        };
    }
}

/**
 * Проверить статус платежа
 * @param {string} paymentId - ID платежа
 */
async function getPaymentStatus(paymentId) {
    try {
        const payment = await checkout.getPayment(paymentId);
        
        return {
            success: true,
            status: payment.status,
            paid: payment.paid,
            amount: payment.amount,
            metadata: payment.metadata
        };
    } catch (error) {
        console.error('[YUKASSA] Get payment status error:', error);
        return {
            success: false,
            message: error.message
        };
    }
}

/**
 * Обработка webhook от ЮKassa
 * @param {object} notification - Данные уведомления
 */
async function handleWebhook(notification) {
    try {
        const { type, object } = notification;
        
        if (type === 'payment.succeeded') {
            const { id, status, amount, metadata } = object;
            
            return {
                success: true,
                paymentId: id,
                status: status,
                amount: parseFloat(amount.value),
                username: metadata.username,
                orderId: metadata.order_id
            };
        }
        
        return {
            success: false,
            message: 'Unknown notification type'
        };
    } catch (error) {
        console.error('[YUKASSA] Webhook error:', error);
        return {
            success: false,
            message: error.message
        };
    }
}

module.exports = {
    createPayment,
    getPaymentStatus,
    handleWebhook
};

