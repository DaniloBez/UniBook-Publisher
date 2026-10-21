package com.unibook.publisher.production.controller;

import com.unibook.publisher.common.enums.UserRole;
import com.unibook.publisher.common.security.AuthenticatedUser;
import com.unibook.publisher.production.entity.request.ManuscriptFormDto;
import com.unibook.publisher.production.entity.request.ManuscriptSubmissionRequest;
import com.unibook.publisher.production.entity.response.ManuscriptResponse;
import com.unibook.publisher.production.enums.ManuscriptStatus;
import com.unibook.publisher.production.service.GenreService;
import com.unibook.publisher.production.service.ManuscriptService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/manuscripts")
public class ManuscriptWebController {

    private static final String LIST_VIEW = "manuscripts/list";
    private static final String FORM_VIEW = "manuscripts/form";

    private final ManuscriptService manuscriptService;
    private final GenreService genreService;

    public ManuscriptWebController(ManuscriptService manuscriptService, GenreService genreService) {
        this.manuscriptService = manuscriptService;
        this.genreService = genreService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) ManuscriptStatus status, Model model) {
        model.addAttribute("heading", "Усі рукописи");
        model.addAttribute("manuscripts", manuscriptService.getManuscripts(status));
        model.addAttribute("genres", genreService.getAllGenres());

        return LIST_VIEW;
    }

    @GetMapping("/my")
    public String myList(@RequestParam(required = false) ManuscriptStatus status, Authentication authentication, @AuthenticationPrincipal AuthenticatedUser currentUser, Model model) {
        UserRole role = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority.startsWith("ROLE_"))
            .map(authority -> UserRole.valueOf(authority.substring("ROLE_".length())))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Authenticated user has no role"));

        List<ManuscriptResponse> manuscripts = manuscriptService.getManuscriptsForUser(currentUser.id(), role)
                .stream()
                .filter(manuscript -> status == null || manuscript.status() == status)
                .toList();

        model.addAttribute("heading", "Мої рукописи");
        model.addAttribute("manuscripts", manuscripts);
        model.addAttribute("genres", genreService.getAllGenres());

        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute(
                "manuscriptForm",
                new ManuscriptFormDto("", "", "", List.of())
        );
        model.addAttribute("genres", genreService.getAllGenres());

        return FORM_VIEW;
    }

    @PostMapping("/new")
    public String create(
            @Valid @ModelAttribute("manuscriptForm") ManuscriptFormDto form,
            BindingResult bindingResult,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("genres", genreService.getAllGenres());
            return FORM_VIEW;
        }

        manuscriptService.submitManuscript(
                currentUser,
                new ManuscriptSubmissionRequest(
                        form.title(),
                        form.annotation(),
                        form.draftFileUrl(),
                        form.genreIds()
                )
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Рукопис успішно подано."
        );

        return "redirect:/manuscripts/my";
    }
}
