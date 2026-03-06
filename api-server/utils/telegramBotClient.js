const { EventEmitter } = require('events');

function sleep(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

class TelegramBotClient extends EventEmitter {
    constructor(token, options = {}) {
        super();
        this.token = token;
        this.polling = Boolean(options.polling);
        this.pollingBackoffMs = options.pollingBackoffMs || 3000;
        this.pollingTimeoutSec = options.pollingTimeoutSec || 25;
        this.updateOffset = 0;
        this.stopped = false;

        if (this.polling) {
            this.startPolling();
        }
    }

    async callApi(method, payload) {
        const response = await fetch(`https://api.telegram.org/bot${this.token}/${method}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload || {})
        });

        if (!response.ok) {
            throw new Error(`Telegram API HTTP ${response.status} on ${method}`);
        }

        const data = await response.json();
        if (!data.ok) {
            throw new Error(`Telegram API error on ${method}: ${data.description || 'unknown'}`);
        }

        return data.result;
    }

    async sendMessage(chatId, text, options = {}) {
        return this.callApi('sendMessage', {
            chat_id: chatId,
            text,
            ...options
        });
    }

    async getMe() {
        return this.callApi('getMe', {});
    }

    startPolling() {
        if (this.stopped) {
            return;
        }

        const poll = async () => {
            if (this.stopped) {
                return;
            }

            try {
                const updates = await this.callApi('getUpdates', {
                    timeout: this.pollingTimeoutSec,
                    offset: this.updateOffset,
                    allowed_updates: ['message']
                });

                for (const update of updates) {
                    this.updateOffset = Math.max(this.updateOffset, update.update_id + 1);
                    if (update.message) {
                        this.emit('message', update.message);
                    }
                }
            } catch (error) {
                this.emit('polling_error', error);
                await sleep(this.pollingBackoffMs);
            }

            setImmediate(poll);
        };

        setImmediate(poll);
    }

    stopPolling() {
        this.stopped = true;
    }
}

module.exports = TelegramBotClient;
