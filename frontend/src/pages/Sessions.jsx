import React, { useEffect, useState } from "react";
import API_URL from "../api";

import "./Sessions.css";

function Sessions() {
    const [sessions, setSessions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [showLogoutConfirm, setShowLogoutConfirm] =
        useState(false);
    const [message, setMessage] = useState("");

    // Get current session ID from JWT
    const getCurrentSessionId = () => {
        const token = localStorage.getItem("token");

        if (!token) {
            return null;
        }

        try {
            const payload = JSON.parse(
                atob(token.split(".")[1])
            );

            return payload.sessionId || null;
        } catch (error) {
            console.error(
                "Unable to read session ID:",
                error
            );

            return null;
        }
    };

    const fetchSessions = async () => {
        const currentToken =
            localStorage.getItem("token");

        if (!currentToken) {
            setLoading(false);
            setMessage("Please login first.");
            return;
        }

        try {
            const response = await fetch(
                `${API_URL}/api/sessions`,
                {
                    method: "GET",
                    headers: {
                        Authorization:
                            `Bearer ${currentToken}`,
                    },
                }
            );

            if (!response.ok) {
                throw new Error(
                    `Sessions request failed: ${response.status}`
                );
            }

            const data = await response.json();

            setSessions(data);
            setMessage("");
        } catch (error) {
            console.error(
                "Sessions error:",
                error
            );

            setMessage(
                "Unable to load sessions."
            );
        } finally {
            setLoading(false);
        }
    };

    const revokeSession = async (sessionId) => {
        const currentToken =
            localStorage.getItem("token");

        if (!currentToken) {
            setMessage("Please login first.");
            return;
        }

        // Prevent revoking current session
        const currentSessionId =
            getCurrentSessionId();

        if (sessionId === currentSessionId) {
            setMessage(
                "You cannot revoke your current session. Use Logout All Sessions instead."
            );

            return;
        }

        try {
            const response = await fetch(
                `${API_URL}/api/sessions/${sessionId}`,
                {
                    method: "DELETE",
                    headers: {
                        Authorization:
                            `Bearer ${currentToken}`,
                    },
                }
            );

            if (response.ok) {
                setMessage(
                    "Session revoked successfully."
                );

                await fetchSessions();
            } else {
                setMessage(
                    "Failed to revoke session."
                );
            }
        } catch (error) {
            console.error(
                "Revoke session error:",
                error
            );

            setMessage(
                "Unable to revoke session."
            );
        }
    };

    const logoutAllSessions = async () => {
        const currentToken =
            localStorage.getItem("token");

        if (!currentToken) {
            setMessage("Please login first.");
            return;
        }

        try {
            const response = await fetch(
                `${API_URL}/api/sessions/logout-all`,
                {
                    method: "POST",
                    headers: {
                        Authorization:
                            `Bearer ${currentToken}`,
                    },
                }
            );

            if (response.ok) {
                localStorage.removeItem("token");

                setShowLogoutConfirm(false);

                window.location.href = "/login";
            } else {
                setMessage(
                    "Failed to logout all sessions."
                );

                setShowLogoutConfirm(false);
            }
        } catch (error) {
            console.error(
                "Logout all error:",
                error
            );

            setMessage(
                "Unable to logout all sessions."
            );

            setShowLogoutConfirm(false);
        }
    };

    useEffect(() => {
        fetchSessions();
    }, []);

    if (loading) {
        return (
            <div className="sessions-page">
                <div className="sessions-loading">
                    Loading sessions...
                </div>
            </div>
        );
    }

    const activeSessions =
        sessions.filter(
            (session) =>
                !session.revoked
        );

    const revokedSessions =
        sessions.filter(
            (session) =>
                session.revoked
        );

    const currentSessionId =
        getCurrentSessionId();

    return (
        <div className="sessions-page">
            <div className="sessions-container">

                {/* HEADER */}
                <div className="sessions-header">
                    <div>
                        <h1>
                            Security Sessions
                        </h1>

                        <p>
                            Manage devices that are
                            currently signed in to your
                            SecureVault account.
                        </p>
                    </div>

                    <button
                        className="logout-all-btn"
                        onClick={() =>
                            setShowLogoutConfirm(
                                true
                            )
                        }
                    >
                        Logout All Sessions
                    </button>
                </div>

                {/* MESSAGE */}
                {message && (
                    <div className="sessions-message">
                        {message}
                    </div>
                )}

                {/* SUMMARY */}
                <div className="session-summary">

                    <div className="summary-card">
                        <span className="summary-number">
                            {activeSessions.length}
                        </span>

                        <span className="summary-label">
                            Active Sessions
                        </span>
                    </div>

                    <div className="summary-card">
                        <span className="summary-number">
                            {revokedSessions.length}
                        </span>

                        <span className="summary-label">
                            Revoked Sessions
                        </span>
                    </div>

                    <div className="summary-card">
                        <span className="summary-number">
                            {sessions.length}
                        </span>

                        <span className="summary-label">
                            Total Sessions
                        </span>
                    </div>

                </div>

                {/* ACTIVE SESSIONS */}
                <h2 className="section-title">
                    Active Sessions
                </h2>

                {activeSessions.length === 0 ? (
                    <div className="empty-card">
                        No active sessions found.
                    </div>
                ) : (
                    <div className="sessions-grid">

                        {activeSessions.map(
                            (session) => {

                                const isCurrent =
                                    session.sessionId ===
                                    currentSessionId;

                                return (
                                    <div
                                        className={`session-card ${
                                            isCurrent
                                                ? "current-card"
                                                : "active-card"
                                        }`}
                                        key={
                                            session.sessionId
                                        }
                                    >

                                        {/* CARD TOP */}
                                        <div className="session-card-top">

                                            <div className="device-icon">
                                                💻
                                            </div>

                                            <span
                                                className={
                                                    isCurrent
                                                        ? "current-badge"
                                                        : "active-badge"
                                                }
                                            >
                                                {isCurrent
                                                    ? "Current Device"
                                                    : "Active"}
                                            </span>

                                        </div>

                                        {/* DEVICE */}
                                        <h3>
                                            {session.device ||
                                                "Unknown Device"}
                                        </h3>

                                        {/* SESSION INFO */}
                                        <div className="session-info">

                                            <p>
                                                <strong>
                                                    IP Address
                                                </strong>

                                                <span>
                                                    {
                                                        session.ipAddress
                                                    }
                                                </span>
                                            </p>

                                            <p>
                                                <strong>
                                                    Created
                                                </strong>

                                                <span>
                                                    {new Date(
                                                        session.createdAt
                                                    ).toLocaleString()}
                                                </span>
                                            </p>

                                            <p>
                                                <strong>
                                                    Expires
                                                </strong>

                                                <span>
                                                    {new Date(
                                                        session.expiresAt
                                                    ).toLocaleString()}
                                                </span>
                                            </p>

                                        </div>

                                        {/* CURRENT DEVICE */}
                                        {isCurrent ? (
                                            <div className="current-session-info">
                                                🔒 You are
                                                currently using
                                                this device.
                                            </div>
                                        ) : (
                                            <button
                                                className="revoke-btn"
                                                onClick={() =>
                                                    revokeSession(
                                                        session.sessionId
                                                    )
                                                }
                                            >
                                                Revoke Session
                                            </button>
                                        )}

                                    </div>
                                );
                            }
                        )}

                    </div>
                )}

                {/* REVOKED SESSIONS */}
                {revokedSessions.length > 0 && (
                    <>
                        <h2 className="section-title revoked-title">
                            Revoked Sessions
                        </h2>

                        <div className="sessions-grid">

                            {revokedSessions.map(
                                (session) => (
                                    <div
                                        className="session-card revoked-card"
                                        key={
                                            session.sessionId
                                        }
                                    >

                                        <div className="session-card-top">

                                            <div className="device-icon">
                                                💻
                                            </div>

                                            <span className="revoked-badge">
                                                Revoked
                                            </span>

                                        </div>

                                        <h3>
                                            {session.device ||
                                                "Unknown Device"}
                                        </h3>

                                        <div className="session-info">

                                            <p>
                                                <strong>
                                                    IP Address
                                                </strong>

                                                <span>
                                                    {
                                                        session.ipAddress
                                                    }
                                                </span>
                                            </p>

                                            <p>
                                                <strong>
                                                    Created
                                                </strong>

                                                <span>
                                                    {new Date(
                                                        session.createdAt
                                                    ).toLocaleString()}
                                                </span>
                                            </p>

                                            <p>
                                                <strong>
                                                    Expires
                                                </strong>

                                                <span>
                                                    {new Date(
                                                        session.expiresAt
                                                    ).toLocaleString()}
                                                </span>
                                            </p>

                                        </div>

                                    </div>
                                )
                            )}

                        </div>
                    </>
                )}

            </div>

            {/* LOGOUT ALL CONFIRMATION */}
            {showLogoutConfirm && (
                <div className="modal-overlay">

                    <div className="confirm-modal">

                        <h2>
                            Logout all sessions?
                        </h2>

                        <p>
                            This will revoke all active
                            sessions and sign you out of
                            SecureVault.
                        </p>

                        <div className="modal-actions">

                            <button
                                className="cancel-btn"
                                onClick={() =>
                                    setShowLogoutConfirm(
                                        false
                                    )
                                }
                            >
                                Cancel
                            </button>

                            <button
                                className="confirm-btn"
                                onClick={
                                    logoutAllSessions
                                }
                            >
                                Logout All
                            </button>

                        </div>

                    </div>

                </div>
            )}

        </div>
    );
}

export default Sessions;