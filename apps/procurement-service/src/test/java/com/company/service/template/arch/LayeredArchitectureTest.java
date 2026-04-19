package com.manpou.procurement.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * ArchUnit 架构规则测试。
 * 每次构建必须通过，不符合则 CI 失败。
 *
 * API: ArchUnit 1.3.0 + JDK 25
 * - LayerDependencySpecification: mayOnlyBeAccessedByLayers / mayOnlyAccessLayers / mayNotAccessAnyLayer
 *
 * 架构约束：
 * - Application 只能被 Interfaces 访问（禁止外部直接调用）
 * - Application 只能访问 Domain 和 Common（禁止直接访问 Infrastructure）
 * - Domain 只能被 Application 和 Infrastructure 访问
 *   （Infrastructure 实现 Domain 层 Port 接口，属于依赖倒置 + JPA Repository 实现层固有需求）
 * - 无循环依赖
 */
class LayeredArchitectureTest {

    private static final String BASE = "com.manpou.procurement";
    private JavaClasses classes;

    @BeforeEach
    void importClasses() {
        classes = new ClassFileImporter().importPackages(BASE + "..");
    }

    @Test
    void layeredArchitectureCheck() {
        ArchRule rule = layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Application").definedBy(BASE + ".application..")
                .layer("Domain").definedBy(BASE + ".domain..")
                .layer("Infrastructure").definedBy(BASE + ".infrastructure..")
                .layer("Interfaces").definedBy(BASE + ".interfaces..")
                .layer("Common").definedBy(BASE + ".common..")
                // 核心约束：Application 只能被 Interfaces 访问
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Interfaces")
                // 核心约束：Application 只能访问 Common 和 Domain（禁止直接访问 Infrastructure）
                // 通过 SigningKeyPort（Domain 层）实现依赖倒置
                .whereLayer("Application").mayOnlyAccessLayers("Common", "Domain")
                // Domain：禁止 Interfaces 直接操作（必须经 Application 编排）
                // Infrastructure 可访问 Domain（实现 Domain Port 接口 + JPA Repository 实现层固有需求）
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure");

        rule.check(classes);
    }

    @Test
    void noCyclicDependenciesCheck() {
        ArchRule rule = slices()
                .matching(BASE + ".(*)..")
                .should().beFreeOfCycles();

        rule.check(classes);
    }
}
