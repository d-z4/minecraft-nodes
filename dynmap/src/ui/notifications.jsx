/**
 * Notification system for displaying warnings, errors, and info messages
 * over the Nodes Viewer panel.
 */

"use strict";

import { useState, useEffect, useCallback } from "react";
import "./css/notifications.css";

// Notification types
export const NotificationType = {
    INFO: "info",
    WARN: "warn",
    ERROR: "error",
};
// Nodes.warn("This is a warning message");
// Nodes.error("This is an error message");
// Nodes.info("This is an info message");
// Single notification component
const Notification = ({ id, type, message, onDismiss, duration }) => {
    useEffect(() => {
        if (duration > 0) {
            const timer = setTimeout(() => {
                onDismiss(id);
            }, duration);
            return () => clearTimeout(timer);
        }
    }, [id, duration, onDismiss]);

    const typeClass = `nodes-notification-${type}`;

    return (
        <div className={`nodes-notification ${typeClass}`}>
            <div className="nodes-notification-content">
                <span className="nodes-notification-icon">
                    {type === NotificationType.ERROR && "✕"}
                    {type === NotificationType.WARN && "⚠"}
                    {type === NotificationType.INFO && "🧾"}
                </span>
                <span className="nodes-notification-message">{message}</span>
            </div>
            <button 
                className="nodes-notification-close"
                onClick={() => onDismiss(id)}
                aria-label="Dismiss"
            >
                ×
            </button>
        </div>
    );
};

// Notifications container component
export const NotificationsContainer = ({ notifications, onDismiss }) => {
    if (!notifications || notifications.length === 0) {
        return null;
    }

    return (
        <div className="nodes-notifications-container">
            {notifications.map((notif) => (
                <Notification
                    key={notif.id}
                    id={notif.id}
                    type={notif.type}
                    message={notif.message}
                    duration={notif.duration}
                    onDismiss={onDismiss}
                />
            ))}
        </div>
    );
};

// Notification manager class - handles the notification state
let notificationIdCounter = 0;
let notificationUpdateCallback = null;
let currentNotifications = [];

export const NotificationManager = {
    // Set the callback that will trigger React state updates
    setUpdateCallback: (callback) => {
        notificationUpdateCallback = callback;
    },

    // Get current notifications
    getNotifications: () => currentNotifications,

    // Add a notification
    add: (type, message, duration = 5000) => {
        const id = ++notificationIdCounter;
        const notification = { id, type, message, duration };
        
        currentNotifications = [...currentNotifications, notification];
        
        if (notificationUpdateCallback) {
            notificationUpdateCallback(currentNotifications);
        }

        return id;
    },

    // Remove a notification by id
    dismiss: (id) => {
        currentNotifications = currentNotifications.filter(n => n.id !== id);
        
        if (notificationUpdateCallback) {
            notificationUpdateCallback(currentNotifications);
        }
    },

    // Clear all notifications
    clearAll: () => {
        currentNotifications = [];
        
        if (notificationUpdateCallback) {
            notificationUpdateCallback(currentNotifications);
        }
    },

    // Convenience methods
    info: (message, duration = 4000) => {
        return NotificationManager.add(NotificationType.INFO, message, duration);
    },

    warn: (message, duration = 6000) => {
        return NotificationManager.add(NotificationType.WARN, message, duration);
    },

    error: (message, duration = 8000) => {
        return NotificationManager.add(NotificationType.ERROR, message, duration);
    },
};

// Hook for using notifications in components
export const useNotifications = () => {
    const [notifications, setNotifications] = useState(currentNotifications);

    useEffect(() => {
        NotificationManager.setUpdateCallback(setNotifications);
        return () => NotificationManager.setUpdateCallback(null);
    }, []);

    const dismiss = useCallback((id) => {
        NotificationManager.dismiss(id);
    }, []);

    return { notifications, dismiss };
};
