package com.zym.hd.experiment.controller;

import com.zym.hd.course.entity.CourseMember;
import com.zym.hd.course.service.CourseMemberService;
import com.zym.hd.experiment.entity.ExperimentEntity;
import com.zym.hd.experiment.service.ExperimentService;
import com.zym.hd.file.entity.FileEntity;
import com.zym.hd.file.service.FileService;
import com.zym.hd.file.service.OssService;
import com.zym.hd.security.LoginUser;
import com.zym.hd.security.SecurityContextUtil;
import com.zym.hd.user.entity.UserEntity;
import com.zym.hd.user.service.UserService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/experiment")
public class ExperimentController {

    private final ExperimentService experimentService;
    private final UserService userService;
    private final CourseMemberService courseMemberService;
    private final OssService ossService;
    private final FileService fileService;

    public ExperimentController(ExperimentService experimentService,
                                UserService userService,
                                CourseMemberService courseMemberService,
                                OssService ossService,
                                FileService fileService) {
        this.experimentService = experimentService;
        this.userService = userService;
        this.courseMemberService = courseMemberService;
        this.ossService = ossService;
        this.fileService = fileService;
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping("/create")
    public ExperimentEntity create(@RequestBody ExperimentEntity entity) {
        return experimentService.createExperiment(entity, SecurityContextUtil.currentUserId());
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PutMapping("/update")
    public ExperimentEntity update(@RequestBody ExperimentEntity entity) {
        return experimentService.updateExperiment(entity);
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping("/uploadAttachment")
    public Map<String, String> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "experimentId", required = false) Long experimentId) {
        String url = ossService.upload(file, "experiment");
        FileEntity fileEntity = fileService.createOssAsset(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename(),
                url,
                file.getContentType(),
                file.getSize(),
                "EXPERIMENT_ATTACHMENT",
                experimentId,
                SecurityContextUtil.currentUserId());
        return Map.of(
                "id", String.valueOf(fileEntity.getId()),
                "url", url,
                "name", fileEntity.getOriginalName() == null ? "" : fileEntity.getOriginalName());
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return experimentService.deleteExperiment(id);
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping("/getById")
    public ExperimentEntity getById(@RequestParam("id") Long id) {
        return experimentService.getById(id);
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping("/list")
    public List<ExperimentEntity> list() {
        LoginUser loginUser = SecurityContextUtil.currentUser();
        String role = loginUser.getRole();
        UserEntity me = userService.getById(loginUser.getUserId());

        if ("TEACHER".equals(role) || "ADMIN".equals(role)) {
            String teacherNo = me != null ? me.getTeacherNo() : null;
            if (teacherNo == null) {
                return new ArrayList<>();
            }
            return experimentService.lambdaQuery()
                    .eq(ExperimentEntity::getDeleted, 0)
                    .eq(ExperimentEntity::getCreatorNo, teacherNo)
                    .list();
        }

        String studentNo = me != null ? me.getStudentNo() : null;
        if (studentNo == null) {
            return new ArrayList<>();
        }
        List<CourseMember> memberships = courseMemberService.lambdaQuery()
                .eq(CourseMember::getUserNo, studentNo)
                .eq(CourseMember::getRoleInCourse, "STUDENT")
                .list();
        if (memberships.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> courseIds = memberships.stream()
                .map(CourseMember::getCourseId)
                .collect(Collectors.toList());
        return experimentService.lambdaQuery()
                .eq(ExperimentEntity::getDeleted, 0)
                .in(ExperimentEntity::getCourseId, courseIds)
                .list();
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping("/listByCourse")
    public List<ExperimentEntity> listByCourse(@RequestParam("courseId") Long courseId) {
        return experimentService.lambdaQuery()
                .eq(ExperimentEntity::getDeleted, 0)
                .eq(ExperimentEntity::getCourseId, courseId)
                .list();
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/submit")
    public ExperimentEntity submit(@RequestParam("id") Long id) {
        return experimentService.submitExperiment(id, SecurityContextUtil.currentUserId());
    }
}
