package com.securevault.service;

import com.securevault.entity.SecureNote;
import com.securevault.entity.User;
import com.securevault.repository.SecureNoteRepository;
import com.securevault.security.EncryptionService;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecureNoteService {

    private final SecureNoteRepository secureNoteRepository;
    private final EncryptionService encryptionService;
    private final SecurityEventService securityEventService;

    public SecureNoteService(
            SecureNoteRepository secureNoteRepository,
            EncryptionService encryptionService,
            SecurityEventService securityEventService) {

        this.secureNoteRepository = secureNoteRepository;
        this.encryptionService = encryptionService;
        this.securityEventService = securityEventService;
    }

    // CREATE SECURE NOTE
    public SecureNote addNote(
            SecureNote note,
            User user) {

        if (note.getTitle() == null ||
                note.getTitle().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Title is required");
        }

        if (note.getContent() == null ||
                note.getContent().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Content is required");
        }

        note.setContent(
                encryptionService.encrypt(
                        note.getContent()));

        note.setUser(user);

        SecureNote savedNote =
                secureNoteRepository.save(note);

        securityEventService.recordEvent(
                user.getEmail(),
                "SECURE_NOTE_ADDED",
                "N/A",
                "N/A"
        );

        return decryptNote(savedNote);
    }

    // GET OWN SECURE NOTES
    public List<SecureNote> getUserNotes(
            User user) {

        List<SecureNote> notes =
                secureNoteRepository.findByUser(user);

        for (SecureNote note : notes) {
            decryptContent(note);
        }

        return notes;
    }

    // GET SINGLE NOTE
    public SecureNote getNoteById(
            Long noteId,
            User user) {

        SecureNote note =
                secureNoteRepository.findById(noteId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Secure note not found"));

        if (!note.getUser().getId()
                .equals(user.getId())) {

            throw new IllegalArgumentException(
                    "You are not allowed to access this secure note");
        }

        return decryptNote(note);
    }

    // UPDATE OWN SECURE NOTE
    public SecureNote updateNote(
            Long noteId,
            SecureNote updatedNote,
            User user) {

        SecureNote existingNote =
                secureNoteRepository.findById(noteId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Secure note not found"));

        if (!existingNote.getUser().getId()
                .equals(user.getId())) {

            throw new IllegalArgumentException(
                    "You are not allowed to update this secure note");
        }

        if (updatedNote.getTitle() == null ||
                updatedNote.getTitle().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Title is required");
        }

        if (updatedNote.getContent() == null ||
                updatedNote.getContent().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Content is required");
        }

        existingNote.setTitle(
                updatedNote.getTitle());

        existingNote.setContent(
                encryptionService.encrypt(
                        updatedNote.getContent()));

        SecureNote savedNote =
                secureNoteRepository.save(existingNote);

        securityEventService.recordEvent(
                user.getEmail(),
                "SECURE_NOTE_UPDATED",
                "N/A",
                "N/A"
        );

        return decryptNote(savedNote);
    }

    // DELETE OWN SECURE NOTE
    public void deleteNote(
            Long noteId,
            User user) {

        SecureNote note =
                secureNoteRepository.findById(noteId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Secure note not found"));

        if (!note.getUser().getId()
                .equals(user.getId())) {

            throw new IllegalArgumentException(
                    "You are not allowed to delete this secure note");
        }

        secureNoteRepository.delete(note);

        securityEventService.recordEvent(
                user.getEmail(),
                "SECURE_NOTE_DELETED",
                "N/A",
                "N/A"
        );
    }

    // DECRYPT NOTE
    private SecureNote decryptNote(
            SecureNote note) {

        decryptContent(note);
        return note;
    }

    private void decryptContent(
            SecureNote note) {

        try {

            note.setContent(
                    encryptionService.decrypt(
                            note.getContent()));

        } catch (Exception e) {

            System.out.println(
                    "DECRYPTION FAILED FOR SECURE NOTE ID: "
                            + note.getId());

            note.setContent(null);
        }
    }
}
