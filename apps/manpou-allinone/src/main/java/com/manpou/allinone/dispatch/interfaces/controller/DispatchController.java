package com.manpou.allinone.dispatch.interfaces.controller;

import com.manpou.allinone.common.annotation.AuditLog;
import com.manpou.allinone.common.annotation.Idempotent;
import com.manpou.allinone.common.result.Result;
import com.manpou.allinone.dispatch.application.dto.DispatchBatchStatusCmd;
import com.manpou.allinone.dispatch.application.dto.DispatchCreateCmd;
import com.manpou.allinone.dispatch.application.dto.DispatchQuery;
import com.manpou.allinone.dispatch.application.dto.DispatchUpdateCmd;
import com.manpou.allinone.dispatch.application.dto.DispatchVO;
import com.manpou.allinone.dispatch.application.usecase.DispatchUseCase;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dispatches")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchUseCase useCase;

    @GetMapping
    @PreAuthorize("hasAuthority('dispatch:read')")
    public Result<Page<DispatchVO>> list(DispatchQuery query) {
        return Result.ok(useCase.pageQuery(query));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('dispatch:export')")
    @AuditLog(module = "dispatch", action = "EXPORT", resourceType = "dispatch", resourceId = "")
    public void exportCsv(DispatchQuery query, HttpServletResponse response) throws IOException {
        List<DispatchVO> data = useCase.exportAll(query);
        String filename = "dispatch_" + java.time.LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".csv";

        response.setContentType("text/csv;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + filename);

        PrintWriter out = response.getWriter();
        // BOM for Excel UTF-8 compatibility
        out.write("\uFEFF");

        // CSV header (same columns as frontend table, no actions)
        out.println("ID,货号,名称,担当者,目的地,报关,材质,检测,数量,件数,毛重,净重,尺,位置,含税单价,票点,工厂地址,交货日期,状态,备注");

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (DispatchVO row : data) {
            out.println(joinCsv(
                    row.getId(),
                    val(row.getCode()),
                    val(row.getProductNameZh()),
                    val(row.getManager()),
                    val(row.getDestination()),
                    val(row.getTax()),
                    val(row.getMaterial()),
                    val(row.getKensa()),
                    row.getQuantity(),
                    row.getPieces(),
                    row.getWeight2(),
                    row.getWeight(),
                    row.getLength(),
                    val(row.getLocation()),
                    row.getUnitPrice(),
                    row.getRate(),
                    val(row.getFactoryAddr()),
                    row.getDispatchDate() != null ? row.getDispatchDate().format(dateFmt) : "",
                    val(row.getStatus()),
                    val(row.getOther())
            ));
        }
        out.flush();
    }

    private static String val(Object v) {
        if (v == null) return "";
        String s = v.toString();
        // Escape double quotes and wrap in quotes if contains comma/quote/newline
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String joinCsv(Object... vals) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vals.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(val(vals[i]));
        }
        return sb.toString();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('dispatch:read')")
    public Result<DispatchVO> get(@PathVariable("id") Long id) {
        return Result.ok(useCase.getById(id));
    }

    @GetMapping("/by-code/{code}")
    @PreAuthorize("hasAuthority('dispatch:read')")
    public Result<DispatchVO> getLatestByCode(@PathVariable("code") String code) {
        return Result.ok(useCase.getLatestByCode(code));
    }

    @PostMapping
    @Idempotent(ttl = 86400)
    @PreAuthorize("hasAuthority('dispatch:create')")
    @AuditLog(module = "dispatch", action = "CREATE", resourceType = "dispatch", resourceId = "#_return")
    public Result<Long> create(@Valid @RequestBody DispatchCreateCmd cmd) {
        return Result.ok(useCase.create(cmd));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('dispatch:update')")
    @AuditLog(module = "dispatch", action = "UPDATE", resourceType = "dispatch", resourceId = "#id")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody DispatchUpdateCmd cmd) {
        useCase.update(id, cmd);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('dispatch:delete')")
    @AuditLog(module = "dispatch", action = "DELETE", resourceType = "dispatch", resourceId = "#id")
    public Result<Void> delete(@PathVariable("id") Long id) {
        useCase.delete(id);
        return Result.ok();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('dispatch:update')")
    @AuditLog(module = "dispatch", action = "STATUS_CHANGE", resourceType = "dispatch", resourceId = "#id")
    public Result<Void> patchStatus(@PathVariable("id") Long id,
                                    @RequestBody java.util.Map<String, String> body) {
        useCase.patchStatus(id, body.get("status"));
        return Result.ok();
    }

    @PatchMapping("/batch-status")
    @PreAuthorize("hasAuthority('dispatch:update')")
    @AuditLog(module = "dispatch", action = "BATCH_STATUS_CHANGE", resourceType = "dispatch", resourceId = "#cmd.ids")
    public Result<Integer> patchBatchStatus(@Valid @RequestBody DispatchBatchStatusCmd cmd) {
        return Result.ok(useCase.patchBatchStatus(cmd.getIds(), cmd.getStatus()));
    }
}