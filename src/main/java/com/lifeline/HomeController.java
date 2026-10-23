
package com.lifeline;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class HomeController {

    private final UserRepository userRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public HomeController(
            UserRepository userRepository,
            HealthRecordRepository healthRecordRepository) {
        this.userRepository = userRepository;
        this.healthRecordRepository = healthRecordRepository;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(
            @RequestParam String name,
            @RequestParam int age,
            @RequestParam String place,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model) {

        if (name.isBlank() || place.isBlank() || username.isBlank()) {
            model.addAttribute("error", "Please fill in all required fields.");
            return "signup";
        }

        if (age < 1 || age > 120) {
            model.addAttribute("error", "Please enter a valid age.");
            return "signup";
        }

        if (password.length() < 8) {
            model.addAttribute("error",
                    "Password must contain at least 8 characters.");
            return "signup";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "signup";
        }

        if (userRepository.existsByUsername(username.trim())) {
            model.addAttribute("error", "Username already exists.");
            return "signup";
        }

        User user = new User();
        user.setName(name.trim());
        user.setAge(age);
        user.setPlace(place.trim());
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));

        userRepository.save(user);

        return "redirect:/login?registered";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        User user = userRepository.findByUsername(username.trim())
                .orElse(null);

        if (user == null ||
                !passwordEncoder.matches(password, user.getPassword())) {
            model.addAttribute("error",
                    "Invalid username or password.");
            return "login";
        }

        session.setAttribute("loggedInUser", user.getId());

        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        return "dashboard";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        User user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        return "profile";
    }

    @GetMapping("/health-dashboard")
    public String healthDashboard(HttpSession session, Model model) {
        User user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);
        model.addAttribute(
                "records",
                healthRecordRepository.findByUserId(user.getId()));

        return "health-dashboard";
    }

    @PostMapping("/health-dashboard")
    public String addHealthRecord(
            @RequestParam String conditionName,
            @RequestParam(required = false, defaultValue = "") String notes,
            HttpSession session,
            Model model) {

        User user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        if (conditionName.isBlank() || conditionName.length() > 100
                || notes.length() > 1000) {
            model.addAttribute("user", user);
            model.addAttribute("records",
                    healthRecordRepository.findByUserId(user.getId()));
            model.addAttribute("error", "Please check your record details.");
            return "health-dashboard";
        }

        HealthRecord record = new HealthRecord();
        record.setUserId(user.getId());
        record.setConditionName(conditionName.trim());
        record.setNotes(notes.trim());

        healthRecordRepository.save(record);

        return "redirect:/health-dashboard";
    }

    @PostMapping("/health-dashboard/delete")
    public String deleteHealthRecord(
            @RequestParam Long id,
            HttpSession session) {

        User user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        HealthRecord record =
                healthRecordRepository.findById(id).orElse(null);

        if (record != null &&
                record.getUserId().equals(user.getId())) {
            healthRecordRepository.delete(record);
        }

        return "redirect:/health-dashboard";
    }

    @GetMapping("/symptoms")
    public String symptomsPage(HttpSession session) {
        return getLoggedInUser(session) == null
                ? "redirect:/login" : "symptom";
    }

    @PostMapping("/symptoms")
    public String checkSymptoms(
            @RequestParam String symptom,
            @RequestParam(required = false, defaultValue = "") String details,
            HttpSession session,
            Model model) {

        if (getLoggedInUser(session) == null) {
            return "redirect:/login";
        }

        if (details.length() > 1000) {
            model.addAttribute("error",
                    "Please keep additional details under 1000 characters.");
            return "symptom";
        }

        String symptomName;
        String information;

        switch (symptom) {
            case "headache" -> {
                symptomName = "Headache";
                information = "Headaches can have many causes, including "
                        + "dehydration, stress, or illness. Seek urgent care "
                        + "for a sudden severe headache or headache with "
                        + "weakness, confusion, or other serious symptoms.";
            }
            case "fever" -> {
                symptomName = "Fever";
                information = "Fever may occur with infections and other "
                        + "conditions. Rest and drink fluids if appropriate. "
                        + "Seek medical advice for persistent or worsening "
                        + "fever, or urgently for severe symptoms.";
            }
            case "cough" -> {
                symptomName = "Cough";
                information = "Coughs can have several causes. Seek medical "
                        + "advice if the cough persists or worsens. Get urgent "
                        + "help for difficulty breathing or severe chest pain.";
            }
            case "stomach-pain" -> {
                symptomName = "Stomach Pain";
                information = "Stomach pain can have many causes and cannot "
                        + "be diagnosed from this form. Severe, sudden, or "
                        + "worsening pain needs urgent medical assessment.";
            }
            case "nausea" -> {
                symptomName = "Nausea";
                information = "Nausea can occur for many reasons. Try small "
                        + "sips of fluid if you can tolerate them. Seek care "
                        + "for persistent vomiting, dehydration, or severe pain.";
            }
            case "fatigue" -> {
                symptomName = "Fatigue";
                information = "Fatigue may be related to sleep, stress, "
                        + "or medical conditions. If it persists or affects "
                        + "daily life, consult a healthcare professional.";
            }
            case "sore-throat" -> {
                symptomName = "Sore Throat";
                information = "A sore throat may have several causes. "
                        + "Seek medical advice if it persists or worsens. "
                        + "Get urgent help for difficulty breathing or swallowing.";
            }
            case "other" -> {
                symptomName = "Other Symptom";
                information = "This symptom needs more context for a useful "
                        + "assessment. A qualified healthcare professional "
                        + "can help determine the cause and suitable care.";
            }
            default -> {
                model.addAttribute("error", "Please select a valid symptom.");
                return "symptom";
            }
        }

        model.addAttribute("symptomName", symptomName);
        model.addAttribute("information", information);
        model.addAttribute("details", details);

        return "symptom-result";
    }

    @GetMapping("/medicines")
    public String medicinesPage(HttpSession session) {
        return getLoggedInUser(session) == null
                ? "redirect:/login" : "medicine";
    }

    @PostMapping("/medicines")
    public String medicineInformation(
            @RequestParam String medicine,
            @RequestParam String purpose,
            HttpSession session,
            Model model) {

        if (getLoggedInUser(session) == null) {
            return "redirect:/login";
        }

        String medicineName;
        String information;

        switch (medicine) {
            case "paracetamol" -> {
                medicineName = "Paracetamol";
                information = "Paracetamol is commonly used for pain and "
                        + "fever. Taking too much can cause severe liver "
                        + "damage. Follow the package instructions or a "
                        + "healthcare professional's advice.";
            }
            case "cetirizine" -> {
                medicineName = "Cetirizine";
                information = "Cetirizine is an antihistamine commonly used "
                        + "for allergy symptoms. It can cause drowsiness. "
                        + "Ask a pharmacist or doctor whether it is suitable "
                        + "for you.";
            }
            case "ibuprofen" -> {
                medicineName = "Ibuprofen";
                information = "Ibuprofen is an anti-inflammatory pain reliever. "
                        + "It may not be suitable for people with certain "
                        + "stomach, kidney, heart, or bleeding problems. "
                        + "Ask a healthcare professional before using it.";
            }
            case "antacid" -> {
                medicineName = "Antacids";
                information = "Antacids can relieve some types of heartburn "
                        + "and indigestion. They may interact with other "
                        + "medicines. Ask a pharmacist about suitable use.";
            }
            case "other" -> {
                medicineName = "Other Medicine";
                information = "Check the medicine leaflet and ask a "
                        + "qualified pharmacist or doctor for reliable "
                        + "information specific to that medicine.";
            }
            default -> {
                model.addAttribute("error", "Please select a valid medicine.");
                return "medicine";
            }
        }

        switch (purpose) {
            case "uses" -> {
                // Keep the general medicine information above.
            }
            case "side-effects" -> information +=
                    " Side effects vary. Read the official leaflet and "
                    + "seek medical advice if you experience concerning symptoms.";
            case "precautions" -> information +=
                    " Tell your healthcare professional about your other "
                    + "medicines, allergies, pregnancy, and medical conditions.";
            default -> {
                model.addAttribute("error", "Please select a valid topic.");
                return "medicine";
            }
        }

        model.addAttribute("medicineName", medicineName);
        model.addAttribute("topic", purpose);
        model.addAttribute("information", information);

        return "medicine-result";
    }

    @GetMapping("/reminders")
    public String remindersPage(HttpSession session) {
        return getLoggedInUser(session) == null
                ? "redirect:/login" : "reminder";
    }

    @GetMapping("/hospitals")
    public String hospitalsPage(HttpSession session) {
        return getLoggedInUser(session) == null
                ? "redirect:/login" : "hospital";
    }

    @GetMapping("/appointments")
    public String appointmentsPage(HttpSession session) {
        return getLoggedInUser(session) == null
                ? "redirect:/login" : "appointments";
    }

    @GetMapping("/blood-connect")
    public String bloodConnectPage(HttpSession session) {
        return getLoggedInUser(session) == null
                ? "redirect:/login" : "blood-connect";
    }

    @GetMapping("/ai-assistant")
    public String aiAssistantPage(HttpSession session) {
        return getLoggedInUser(session) == null
                ? "redirect:/login" : "ai-assistant";
    }

    @GetMapping("/emergency")
    public String emergencyPage(HttpSession session) {
        return getLoggedInUser(session) == null
                ? "redirect:/login" : "emergency";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    private User getLoggedInUser(HttpSession session) {
        Object userId = session.getAttribute("loggedInUser");

        if (!(userId instanceof Long id)) {
            return null;
        }

        User user = userRepository.findById(id).orElse(null);

        if (user == null) {
            session.invalidate();
        }

        return user;
    }
}
