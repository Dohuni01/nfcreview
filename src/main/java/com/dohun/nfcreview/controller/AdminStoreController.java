package com.dohun.nfcreview.controller;

import com.dohun.nfcreview.config.AppProperties;
import com.dohun.nfcreview.controller.form.StoreForm;
import com.dohun.nfcreview.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * (5부) 가게·카드 관리 화면.
 * 폼 제출(POST) 뒤에는 항상 redirect로 다른 화면을 GET 하게 해요(PRG 패턴).
 * 그래야 새로고침해도 같은 가게가 두 번 등록되지 않아요.
 */
@Controller
@RequestMapping("/admin")
public class AdminStoreController {

    private static final int LABEL_MAX = 50;

    private final AdminService adminService;
    private final AppProperties appProperties;

    public AdminStoreController(AdminService adminService, AppProperties appProperties) {
        this.adminService = adminService;
        this.appProperties = appProperties;
    }

    // 폼에서 들어온 글자의 앞뒤 공백을 자르고, 빈 칸은 null로 바꿔요 (복사할 때 딸려온 공백 방지)
    @InitBinder
    void trimStrings(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String home() {
        return "redirect:/admin/stores";
    }

    @GetMapping("/stores")
    public String stores(Model model) {
        model.addAttribute("stores", adminService.findAllStores());
        return "admin/stores";
    }

    @GetMapping("/stores/new")
    public String newStoreForm(Model model) {
        model.addAttribute("form", new StoreForm());
        return "admin/store-form";
    }

    @PostMapping("/stores")
    public String createStore(@Valid @ModelAttribute("form") StoreForm form, BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/store-form"; // 입력값과 에러 메시지를 그대로 든 채 폼을 다시 보여줘요
        }
        Long id = adminService.createStore(form.getName(), form.getReviewUrl(), form.getLandingMode(), form.getLandingUrl());
        redirectAttributes.addFlashAttribute("message", "가게를 등록했어요. 이제 카드를 발급해보세요.");
        return "redirect:/admin/stores/" + id;
    }

    @GetMapping("/stores/{id}")
    public String store(@PathVariable("id") Long id, Model model) {
        model.addAttribute("store", adminService.getStore(id));
        model.addAttribute("cards", adminService.findCards(id));
        model.addAttribute("baseUrl", appProperties.baseUrl());
        return "admin/store";
    }

    @GetMapping("/stores/{id}/edit")
    public String editStoreForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("form", StoreForm.from(adminService.getStore(id)));
        model.addAttribute("storeId", id);
        return "admin/store-form";
    }

    @PostMapping("/stores/{id}/edit")
    public String updateStore(@PathVariable("id") Long id,
                              @Valid @ModelAttribute("form") StoreForm form, BindingResult bindingResult,
                              Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("storeId", id);
            return "admin/store-form";
        }
        adminService.updateStore(id, form.getName(), form.getReviewUrl(), form.getLandingMode(), form.getLandingUrl());
        redirectAttributes.addFlashAttribute("message", "가게 정보를 수정했어요.");
        return "redirect:/admin/stores/" + id;
    }

    @PostMapping("/stores/{id}/cards")
    public String addCard(@PathVariable("id") Long id,
                          @RequestParam(name = "label", required = false) String label,
                          RedirectAttributes redirectAttributes) {
        // 화면의 maxlength는 편의 기능일 뿐, 진짜 검증은 항상 서버에서 해요
        if (label != null && label.length() > LABEL_MAX) {
            redirectAttributes.addFlashAttribute("error", "위치 메모는 " + LABEL_MAX + "자 이내로 적어주세요.");
            return "redirect:/admin/stores/" + id;
        }
        String code = adminService.addCard(id, label);
        redirectAttributes.addFlashAttribute("message", "카드를 발급했어요: " + code);
        return "redirect:/admin/stores/" + id;
    }

    @PostMapping("/stores/{id}/delete")
    public String deleteStore(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        adminService.deleteStore(id);
        redirectAttributes.addFlashAttribute("message", "가게를 삭제했어요.");
        return "redirect:/admin/stores";
    }

    // 상태를 바꾸는 요청은 GET 링크가 아니라 POST 폼으로 (링크 미리보기 봇이 눌러버리는 일 방지 + CSRF 보호)
    @PostMapping("/cards/{cardId}/toggle")
    public String toggleCard(@PathVariable("cardId") Long cardId, RedirectAttributes redirectAttributes) {
        Long storeId = adminService.toggleCard(cardId);
        redirectAttributes.addFlashAttribute("message", "카드 상태를 바꿨어요.");
        return "redirect:/admin/stores/" + storeId;
    }
}
