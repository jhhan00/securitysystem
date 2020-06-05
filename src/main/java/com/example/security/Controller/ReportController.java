package com.example.security.Controller;

import com.example.security.Dao.ReportDao;
import com.example.security.Entity.Report;
import com.example.security.Entity.ReportRepository;
import com.example.security.Entity.Task;
import com.example.security.Entity.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/report")
public class ReportController {
    @Autowired
    ReportRepository reportRepository;

    @Autowired
    ReportDao rd;

    @Autowired
    TaskRepository taskRepository;

    public int LoginOrNot(Authentication authentication) {
        // log.info("auth : " + authentication);
        if(authentication == null)  return -1;
        else return 1;
    }

    @GetMapping // report - 본인 것만 보기 , 단 관리자는 전체 인원 보기
    public String reportList(Model model, Authentication auth) {
        int loginOrNot = LoginOrNot(auth);
        if(loginOrNot == -1) return "redirect:/";

        List<Report> list;
        //System.out.println(auth.getName() + " in report list");
        Map<String, String> authority = rd.getUserInfo(auth.getName());
        String role = authority.get("role");
        if(role.equals("USER")) {
            list = reportRepository.findByUsernameOrderByUpdatedTimeDesc(auth.getName());
            List<Report> noticeList = reportRepository.findByReportTypeOrderByWriteDateDesc("Notice");
            list.addAll(noticeList);
        } else {
            list = reportRepository.findAllByOrderByUpdatedTimeDesc();
        }
        model.addAttribute("list",list);
        model.addAttribute("authority", authority);

        return "report/report_list";
    }

    @GetMapping("/search") // report 검색해서 보기
    public String reportSearch(Authentication auth, Model model, HttpServletRequest request) {
        int loginOrNot = LoginOrNot(auth);
        if(loginOrNot == -1) return "redirect:/";

        Map<String, String> authority = rd.getUserInfo(auth.getName());
        model.addAttribute("authority", authority);

        String type = request.getParameter("search1");
        String finding = request.getParameter("searching2");
        log.info(type + " " + finding);
        List<Report> rlist = new ArrayList<>();

        if(authority.get("role").equals("USER")) { // User인 경우
            switch (type) {
                case "username":
                    rlist = reportRepository.findByUsernameStartsWithOrderByWriteDateDesc(finding);
                    break;
                case "reportTitle":
                    rlist = reportRepository.findByUsernameAndReportTitleContainingOrderByWriteDate(auth.getName(), finding);
                    break;
                case "time":
                    List<Report> rl = reportRepository.findByUsernameOrderByUpdatedTimeDesc(auth.getName());
                    for(Report r : rl) {
                        if(finding.equals(r.getWriteDate().toLocalDate().toString()))
                            rlist.add(r);
                    }
                    break;
                case "type":
                    rlist = reportRepository.findByUsernameAndReportTypeOrderByWriteDateDesc(auth.getName(), finding);
                    break;
                case "state":
                    rlist = reportRepository.findByUsernameAndStateOrderByWriteDateDesc(auth.getName(), finding);
                    break;
            }
        } else { // Admin인 경우
            switch (type) {
                case "username":
                    rlist = reportRepository.findByUsernameStartsWithOrderByWriteDateDesc(finding);
                    break;
                case "reportTitle":
                    rlist = reportRepository.findByReportTitleContainingOrderByWriteDateDesc(finding);
                    break;
                case "time":
                    List<Report> rl = reportRepository.findAllByOrderByUpdatedTimeDesc();
                    for (Report rp : rl) {
                        if (finding.equals(rp.getWriteDate().toLocalDate().toString())) {
                            rlist.add(rp);
                        }
                    }
                    break;
                case "type":
                    rlist = reportRepository.findByReportTypeOrderByWriteDateDesc(finding);
                    break;
                case "state":
                    rlist = reportRepository.findByStateOrderByWriteDateDesc(finding);
                    break;
            }
        }

        model.addAttribute("list",rlist);
        return "report/report_list";
    }

    @GetMapping("/sorting") // report 정렬해서 원하는 것 보기
    public String reportSorting(Authentication auth, Model model, @RequestParam("Big")String big,
                                @RequestParam("Small")String small) {
        int loginOrNot = LoginOrNot(auth);
        if(loginOrNot == -1) return "redirect:/";

        Map<String, String> authority = rd.getUserInfo(auth.getName());
        model.addAttribute("authority", authority);

        System.out.println(big + " , " + small);
        List<Report> rlist = new ArrayList<>();
        if(big.equals("type")) {
            rlist = reportRepository.findByReportTypeOrderByWriteDateDesc(small);
        } else if(big.equals("state")) {
            rlist = reportRepository.findByStateOrderByWriteDateDesc(small);
        }
        model.addAttribute("list",rlist);

        return "report/report_list";
    }

    @GetMapping("/detail/{reportId}") // report 상세보기
    public String reportView(@PathVariable("reportId") long reportid, Model model, Authentication auth,
                             HttpServletRequest request) {
        int loginOrNot = LoginOrNot(auth);
        if(loginOrNot == -1) return "redirect:/";

        Map<String, String> authority = rd.getUserInfo(auth.getName());
        model.addAttribute("authority",authority);

        List<Task> list = this.taskRepository.findByReportId(reportid);
        model.addAttribute("list",list);

        Report rp = this.reportRepository.findByReportId(reportid);
        model.addAttribute("info",rp);
        model.addAttribute("oldUrl", request.getHeader("referer"));

        return "report/report_detail";
    }

    @GetMapping("/requested_only")
    public String OnlyRequestedReport(Authentication auth, Model model) {
        int loginOrNot = LoginOrNot(auth);
        if(loginOrNot == -1) return "redirect:/";

        Map<String, String> authority = rd.getUserInfo(auth.getName());
        model.addAttribute("authority", authority);

        List<Report> rlist = reportRepository.findByStateOrderByUpdatedTimeDesc("Requested");
        model.addAttribute("list", rlist);

        return "report/report_list";
    }

    @GetMapping("/request_state")
    public String reviewReport(@RequestParam("rId")String id) {
        Report rp = reportRepository.findByReportId(Long.parseLong(id));
        rp.setState("Requested");
        reportRepository.save(rp);

        return "redirect:/report/detail/" + id;
    }

    @PostMapping("/delete")
    public String deleteReport(@RequestParam("reportID")String id) {
        long r_id = Long.parseLong(id);
        List<Task> tlist = taskRepository.findByReportId(r_id);
        for(Task t : tlist) {
            taskRepository.delete(t);
        }
        Report r = reportRepository.findByReportId(r_id);
        reportRepository.delete(r);

        return "redirect:/report";
    }

    public int RequestedOrNot(long id) {
        Report r = reportRepository.findByReportId(id);
        if(r.getState().equals("Requested") || r.getState().equals("Approved"))
            return -1;
        return 1;
    }

    @GetMapping("/modify_daily")
    public String modifyDaily(@RequestParam("reportID")String id, Model model, Authentication auth) {
        int loginOrNot = LoginOrNot(auth);
        if(loginOrNot == -1) return "redirect:/";

        long r_id = Long.parseLong(id);
        if(RequestedOrNot(r_id) == -1) return "redirect:/report/detail/" + r_id;

        List<Task> tlist = taskRepository.findByReportId(r_id);
        model.addAttribute("list",tlist);

        Map<String, String> authority = rd.getUserInfo(auth.getName());
        model.addAttribute("authority", authority);
        model.addAttribute("reportID",id);

        return "report/modify_daily";
    }

    @PostMapping("/modify_daily")
    public String modifyDailyAction(HttpServletRequest request, Authentication auth) {
        Enumeration<String> keys = request.getParameterNames();
        String key = keys.nextElement();

        long idx = Long.parseLong(request.getParameter(key));
        log.info(key+"_:_"+idx);

        List<Task> tlist = taskRepository.findByReportId(idx);
        int i=0;

        while(keys.hasMoreElements()) {
            key = keys.nextElement();
            log.info(key+"_:_"+request.getParameter(key));
            Task task = new Task();
            if(i != tlist.size()) {
                task = tlist.get(i);
                task.setDone(request.getParameter(key));
                i++;
            } else {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
                String now = LocalDateTime.now().format(dtf);

                task.setReportId(idx);
                task.setUsername(auth.getName());
                task.setSimpleDate(now);
                task.setReportType("Daily");
                task.setReportKind("Done");
                task.setDone(request.getParameter(key));
//                System.out.println(task);
            }
            taskRepository.save(task);
        }

        return "redirect:/report/detail/"+idx;
    }

    @GetMapping("/modify_yearly")
    public String modifyYearly(@RequestParam("reportID")String id, Model model, Authentication auth) {
        int loginOrNot = LoginOrNot(auth);
        if(loginOrNot == -1) return "redirect:/";

        Map<String, String> authority = rd.getUserInfo(auth.getName());
        model.addAttribute("authority", authority);

        long r_id = Long.parseLong(id);
        if(RequestedOrNot(r_id) == -1) return "redirect:/report/detail/" + r_id;

        List<Task> tlist = taskRepository.findByReportId(r_id);
        model.addAttribute("list",tlist);
        model.addAttribute("reportID",id);

        return "report/modify_yearly";
    }

    @PostMapping("/modify_yearly")
    public String modifyYearlyAction(HttpServletRequest request, Authentication auth) {
        Enumeration<String> keys = request.getParameterNames();
        String key = keys.nextElement();

        long idx = Long.parseLong(request.getParameter(key));

        List<Task> tlist = taskRepository.findByReportId(idx);
        int i=0;
//
        while(keys.hasMoreElements()) {
            key = keys.nextElement();
            log.info(key+"_:_"+request.getParameter(key));
            Task task = new Task();
            if(i != tlist.size()) {
                task = tlist.get(i++);
            } else {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
                String now = LocalDateTime.now().format(dtf);

                task.setReportId(idx);
                task.setUsername(auth.getName());
                task.setSimpleDate(now);
                task.setReportType("Yearly");
                task.setReportKind("project_goal");
            }
            task.setProgress(request.getParameter(key));
            key = keys.nextElement();
            //
            String commentComment = request.getParameter(key);
            if(commentComment.length() >= 2000) commentComment = commentComment.substring(0,1999);
            task.setComment(commentComment);
            //
            key = keys.nextElement();
            task.setProjectStartDate(request.getParameter(key));
            key = keys.nextElement();
            task.setProjectTargetDate(request.getParameter(key));
            key = keys.nextElement();
            task.setQuarter1(request.getParameter(key));
            key = keys.nextElement();
            task.setQuarter2(request.getParameter(key));
            key = keys.nextElement();
            task.setQuarter3(request.getParameter(key));
            key = keys.nextElement();
            task.setQuarter4(request.getParameter(key));

            System.out.println(task);
            taskRepository.save(task);
        }

        return "redirect:/report/detail/"+idx;
    }

    @GetMapping("/modify_monthly")
    public String modifyMonthly(@RequestParam("reportID")String id, Model model, Authentication auth) {
        int loginOrNot = LoginOrNot(auth);
        if(loginOrNot == -1) return "redirect:/";

        Map<String, String> authority = rd.getUserInfo(auth.getName());
        model.addAttribute("authority", authority);

        long r_id = Long.parseLong(id);
        if(RequestedOrNot(r_id) == -1) return "redirect:/report/detail/" + r_id;

        List<Task> tlist = taskRepository.findByReportId(r_id);
        model.addAttribute("list",tlist);
        model.addAttribute("reportID",id);

        return "report/modify_monthly";
    }

    @PostMapping("/modify_monthly")
    public String modifyMonthlyAction(HttpServletRequest request, Authentication auth) {
        Enumeration<String> keys = request.getParameterNames();
        String key = keys.nextElement();

        long idx = Long.parseLong(request.getParameter(key));

        List<Task> tlist = taskRepository.findByReportId(idx);
        int i=0;

        while(keys.hasMoreElements()) {
            key = keys.nextElement();
            log.info(key + "_:_" + request.getParameter(key));
            int loc1 = key.indexOf("done");
            int loc2 = key.indexOf("plan");
            int loc3 = key.indexOf("another");
            Task task = new Task();
            if(loc1 != -1) { // this month result
                if(loc3 == -1) { // 이미 있는 done을 수정
                    task = tlist.get(i++);
                } else { // 새롭게 done 추가
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
                    String now = LocalDateTime.now().format(dtf);

                    task.setReportId(idx);
                    task.setUsername(auth.getName());
                    task.setSimpleDate(now);
                    task.setReportType("Monthly");
                }
                task.setExpectedAchievement(null);
                task.setReportKind("Done");
                task.setDone(request.getParameter(key));
                key = keys.nextElement();
                task.setRealAchievement(request.getParameter(key));
                key = keys.nextElement();
                task.setProjectStartDate(request.getParameter(key));
                key = keys.nextElement();
                task.setProjectTargetDate(request.getParameter(key));
                key = keys.nextElement();
                task.setProgress(request.getParameter(key));
                key = keys.nextElement();
                //
                String commentComment = request.getParameter(key);
                if(commentComment.length() >= 2000) commentComment = commentComment.substring(0,1999);
                //
                task.setComment(commentComment);
                key = keys.nextElement();
                task.setQuarter1(request.getParameter(key));
                key = keys.nextElement();
                task.setQuarter2(request.getParameter(key));
                key = keys.nextElement();
                task.setQuarter3(request.getParameter(key));
                key = keys.nextElement();
                task.setQuarter4(request.getParameter(key));
            } else if(loc2 != -1) { // next month plan
                if(loc3 == -1) { // 이미 있는 plan을 수정
                    task = tlist.get(i++);
                } else { // 새롭게 plan 추가
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
                    String now = LocalDateTime.now().format(dtf);

                    task.setReportId(idx);
                    task.setUsername(auth.getName());
                    task.setSimpleDate(now);
                    task.setReportType("Monthly");
                }
                task.setDone(null); task.setRealAchievement(null); task.setProjectStartDate(null); task.setProjectTargetDate(null);
                task.setQuarter1(null); task.setQuarter2(null); task.setQuarter3(null); task.setQuarter4(null);
                task.setReportKind("Next_Month_plan");
                task.setProgress(request.getParameter(key));
                key = keys.nextElement();
                task.setExpectedAchievement(request.getParameter(key));
                key = keys.nextElement();
                //
                String commentComment = request.getParameter(key);
                if(commentComment.length() >= 2000) commentComment = commentComment.substring(0,1999);
                //
                task.setComment(commentComment);
            }
            System.out.println(task);
            taskRepository.save(task);
        }

        return "redirect:/report/detail/"+idx;
    }

    @GetMapping("/modify_weekly")
    public String modifyWeekly(@RequestParam("reportID")String id, Model model, Authentication auth) {
        int loginOrNot = LoginOrNot(auth);
        if(loginOrNot == -1) return "redirect:/";

        Map<String, String> authority = rd.getUserInfo(auth.getName());
        model.addAttribute("authority", authority);

        long r_id = Long.parseLong(id);
        if(RequestedOrNot(r_id) == -1) return "redirect:/report/detail/" + r_id;

        List<Task> tlist = taskRepository.findByReportId(r_id);
        model.addAttribute("list",tlist);
        model.addAttribute("reportID",id);

        return "report/modify_weekly";
    }

    @PostMapping("/modify_weekly")
    public String modifyWeeklyAction(HttpServletRequest request, Authentication auth) {
        Enumeration<String> keys = request.getParameterNames();
        String key = keys.nextElement();

        long idx = Long.parseLong(request.getParameter(key));
        List<Task> tlist = taskRepository.findByReportId(idx);
        int i=0;
        log.info("r_id_:_"+idx);

        while(keys.hasMoreElements()) {
            key = keys.nextElement();
            log.info(key + "_:_" + request.getParameter(key));
            int loc1 = key.indexOf("done");
            int loc2 = key.indexOf("plan");
            int loc3 = key.indexOf("another");
            Task task = new Task();
            if(loc1 != -1) { // this week result or done
                if(loc3 == -1) {
                    task = tlist.get(i++);
                } else {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
                    String now = LocalDateTime.now().format(dtf);

                    task.setReportId(idx);
                    task.setUsername(auth.getName());
                    task.setSimpleDate(now);
                    task.setReportType("Weekly");
                }
                task.setReportKind("weekly_result");
                task.setProgress(null); task.setExpectedAchievement(null);
                task.setDone(request.getParameter(key));
                key = keys.nextElement();
                task.setRealAchievement(request.getParameter(key));
                key = keys.nextElement();
                task.setComment(request.getParameter(key));
                System.out.println(task);
            } else if(loc2 != -1) { // next week plan
                if(loc3 == -1) {
                    task = tlist.get(i++);
                } else {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
                    String now = LocalDateTime.now().format(dtf);

                    task.setReportId(idx);
                    task.setUsername(auth.getName());
                    task.setSimpleDate(now);
                    task.setReportType("Weekly");
                }
                task.setReportKind("weekly_plan");
                task.setDone(null); task.setRealAchievement(null);
                task.setProgress(request.getParameter(key));
                key = keys.nextElement();
                task.setExpectedAchievement(request.getParameter(key));
                key = keys.nextElement();
                task.setComment(request.getParameter(key));
                System.out.println(task);
            }
            taskRepository.save(task);
        }

        return "redirect:/report/detail/"+idx;
    }
}
