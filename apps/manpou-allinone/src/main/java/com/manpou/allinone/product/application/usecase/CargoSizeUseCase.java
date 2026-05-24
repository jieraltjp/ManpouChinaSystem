package com.manpou.allinone.product.application.usecase;

import com.manpou.allinone.common.annotation.AuditLog;
import com.manpou.allinone.common.exception.BusinessException;
import com.manpou.allinone.infrastructure.security.JwtContextHolder;
import com.manpou.allinone.product.application.dto.CargoSizeCreateCmd;
import com.manpou.allinone.product.application.dto.CargoSizePromoteCmd;
import com.manpou.allinone.product.application.dto.CargoSizeUpdateCmd;
import com.manpou.allinone.product.application.dto.CargoSizeVO;
import com.manpou.allinone.product.domain.model.CargoSize;
import com.manpou.allinone.product.domain.model.CargoSizeStatus;
import com.manpou.allinone.product.domain.model.Product;
import com.manpou.allinone.product.domain.model.ProductCategory;
import com.manpou.allinone.product.domain.model.ProductFactory;
import com.manpou.allinone.product.domain.repository.CargoSizeRepository;
import com.manpou.allinone.product.domain.repository.ProductRepository;
import com.manpou.allinone.product.infrastructure.persistence.jpa.ProductFactoryJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CargoSizeUseCase {

    private final CargoSizeRepository cargoSizeRepository;
    private final ProductRepository productRepository;
    private final ProductFactoryJpaRepository productFactoryJpaRepository;

    @Transactional(readOnly = true)
    public Page<CargoSizeVO> query(String keyword, CargoSizeStatus status, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<CargoSize> result;
        if (status != null && keyword != null && !keyword.isBlank()) {
            result = cargoSizeRepository.findByStatusAndCodeContainingIgnoreCase(status, keyword, pr);
        } else if (status != null) {
            result = cargoSizeRepository.findByStatus(status, pr);
        } else if (keyword != null && !keyword.isBlank()) {
            result = cargoSizeRepository.findByCodeContainingIgnoreCase(keyword, pr);
        } else {
            result = cargoSizeRepository.findAll(pr);
        }
        return result.map(CargoSizeVO::from);
    }

    @Transactional(readOnly = true)
    public CargoSizeVO getById(Long id) {
        return cargoSizeRepository.findById(id)
                .map(CargoSizeVO::from)
                .orElseThrow(() -> BusinessException.notFound("CargoSize", id));
    }

    @Transactional
    @AuditLog(module = "cargo_size", action = "CREATE", resourceType = "cargo_size",
            resourceId = "0", resourceCode = "#cmd.code")
    public CargoSizeVO create(CargoSizeCreateCmd cmd) {
        CargoSize entity = new CargoSize();
        entity.setMasterCode(cmd.getMasterCode());
        if (cmd.getSubCode() != null) entity.setSubCode(cmd.getSubCode());
        entity.setCode(cmd.getCode());
        entity.setInputUser(JwtContextHolder.getUsername());
        entity.setUpdateTime(LocalDateTime.now());
        if (cmd.getLengthCm() != null) entity.setLengthCm(cmd.getLengthCm());
        if (cmd.getWidthCm() != null) entity.setWidthCm(cmd.getWidthCm());
        if (cmd.getHeightCm() != null) entity.setHeightCm(cmd.getHeightCm());
        if (cmd.getNetWeightKg() != null) entity.setNetWeightKg(cmd.getNetWeightKg());
        if (cmd.getPackHeightCm() != null) entity.setPackHeightCm(cmd.getPackHeightCm());
        if (cmd.getPackWidthCm() != null) entity.setPackWidthCm(cmd.getPackWidthCm());
        if (cmd.getPackDepthCm() != null) entity.setPackDepthCm(cmd.getPackDepthCm());
        if (cmd.getPackageWeightKg() != null) entity.setPackageWeightKg(cmd.getPackageWeightKg());
        if (cmd.getUnitsPerPackage() != null) entity.setUnitsPerPackage(cmd.getUnitsPerPackage());
        if (cmd.getRemarks() != null) entity.setRemarks(cmd.getRemarks());

        CargoSize saved = cargoSizeRepository.save(entity);
        log.info("货物尺寸新增: cargoSize={}, operator={}", saved.getCode(), JwtContextHolder.getUsername());
        return CargoSizeVO.from(saved);
    }

    @Transactional
    @AuditLog(module = "cargo_size", action = "PROMOTE", resourceType = "cargo_size",
            resourceId = "#id", resourceCode = "#cmd.nameZh")
    public CargoSizeVO promote(Long id, CargoSizePromoteCmd cmd) {
        CargoSize cargoSize = cargoSizeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("CargoSize", id));

        if (cargoSize.getStatus() != CargoSizeStatus.PENDING) {
            throw BusinessException.conflict("货物尺寸状态不是待处理，无法升格");
        }

        // 创建 Product
        Product product = new Product();
        product.setMasterCode(cargoSize.getMasterCode());
        if (cargoSize.getSubCode() != null && !cargoSize.getSubCode().isBlank()) {
            product.setSubCode(cargoSize.getSubCode());
        }
        product.setNameZh(cmd.getNameZh());
        if (cmd.getNameEn() != null) product.setNameEn(cmd.getNameEn());
        if (cmd.getCategory() != null) {
            try {
                product.setCategory(ProductCategory.valueOf(cmd.getCategory()));
            } catch (IllegalArgumentException e) {
                product.setCategory(ProductCategory.ORDINARY);
            }
        }
        if (cmd.getUnit() != null) product.setUnit(cmd.getUnit());
        if (cmd.getUnitPriceRmb() != null) product.setUnitPriceRmb(cmd.getUnitPriceRmb());
        if (cmd.getOrigin() != null) product.setOrigin(cmd.getOrigin());
        if (cmd.getHsCode() != null) product.setHsCode(cmd.getHsCode());

        // 尺寸字段
        product.setLengthCm(cargoSize.getLengthCm());
        product.setWidthCm(cargoSize.getWidthCm());
        product.setHeightCm(cargoSize.getHeightCm());
        product.setNetWeightKg(cargoSize.getNetWeightKg());
        product.setPackageHeightCm(cargoSize.getPackHeightCm());
        product.setPackageWidthCm(cargoSize.getPackWidthCm());
        product.setPackageLengthCm(cargoSize.getPackDepthCm());
        product.setPackageWeightKg(cargoSize.getPackageWeightKg());
        product.setUnitsPerPackage(cargoSize.getUnitsPerPackage());
        if (cargoSize.getLengthCm() != null && cargoSize.getWidthCm() != null && cargoSize.getHeightCm() != null) {
            product.calculateVolume();
        }
        if (cargoSize.getPackHeightCm() != null && cargoSize.getPackWidthCm() != null && cargoSize.getPackDepthCm() != null) {
            product.calculatePackageVolume();
        }

        // 备注
        String remarks = cmd.getRemarks();
        if (remarks != null && !remarks.isBlank()) {
            product.setRemarks("[item_size 升格] " + remarks);
        } else {
            product.setRemarks("[item_size 升格] code=" + cargoSize.getCode());
        }

        // 保存
        Product saved = productRepository.save(product);

        // 关联工厂
        if (cmd.getFactoryIds() != null && !cmd.getFactoryIds().isEmpty()) {
            List<ProductFactory> links = new ArrayList<>();
            for (Long factoryId : cmd.getFactoryIds()) {
                ProductFactory pf = new ProductFactory();
                pf.setProductId(saved.getId());
                pf.setFactoryId(factoryId);
                pf.setIsPreferred(false);
                pf.setCreateTime(LocalDateTime.now());
                pf.setUpdateTime(LocalDateTime.now());
                links.add(pf);
            }
            productFactoryJpaRepository.saveAll(links);
        }

        // 更新 cargo_size
        String operator = JwtContextHolder.getUsername();
        cargoSize.promote(saved.getId(), operator);
        CargoSize updated = cargoSizeRepository.save(cargoSize);

        log.info("货物尺寸升格: cargoSize={}, product={}, operator={}",
                cargoSize.getCode(), saved.getId(), operator);
        return CargoSizeVO.from(updated);
    }

    @Transactional
    @AuditLog(module = "cargo_size", action = "DISCARD", resourceType = "cargo_size",
            resourceId = "#id", resourceCode = "")
    public CargoSizeVO discard(Long id) {
        CargoSize cargoSize = cargoSizeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("CargoSize", id));

        if (cargoSize.getStatus() == CargoSizeStatus.DISCARDED) {
            throw BusinessException.conflict("货物尺寸已废弃，无需重复操作");
        }

        cargoSize.discard();
        CargoSize saved = cargoSizeRepository.save(cargoSize);
        log.info("货物尺寸废弃: cargoSize={}", cargoSize.getCode());
        return CargoSizeVO.from(saved);
    }

    @Transactional
    @AuditLog(module = "cargo_size", action = "UPDATE", resourceType = "cargo_size",
            resourceId = "#id", resourceCode = "")
    public CargoSizeVO update(Long id, CargoSizeUpdateCmd cmd) {
        CargoSize cargoSize = cargoSizeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("CargoSize", id));

        if (cmd.getLengthCm() != null) cargoSize.setLengthCm(cmd.getLengthCm());
        if (cmd.getWidthCm() != null) cargoSize.setWidthCm(cmd.getWidthCm());
        if (cmd.getHeightCm() != null) cargoSize.setHeightCm(cmd.getHeightCm());
        if (cmd.getNetWeightKg() != null) cargoSize.setNetWeightKg(cmd.getNetWeightKg());
        if (cmd.getPackHeightCm() != null) cargoSize.setPackHeightCm(cmd.getPackHeightCm());
        if (cmd.getPackWidthCm() != null) cargoSize.setPackWidthCm(cmd.getPackWidthCm());
        if (cmd.getPackDepthCm() != null) cargoSize.setPackDepthCm(cmd.getPackDepthCm());
        if (cmd.getPackageWeightKg() != null) cargoSize.setPackageWeightKg(cmd.getPackageWeightKg());
        if (cmd.getUnitsPerPackage() != null) cargoSize.setUnitsPerPackage(cmd.getUnitsPerPackage());
        if (cmd.getRemarks() != null) cargoSize.setRemarks(cmd.getRemarks());

        CargoSize saved = cargoSizeRepository.save(cargoSize);
        log.info("货物尺寸更新: cargoSize={}", cargoSize.getCode());
        return CargoSizeVO.from(saved);
    }

    @Transactional
    @AuditLog(module = "cargo_size", action = "DELETE", resourceType = "cargo_size",
            resourceId = "#id", resourceCode = "")
    public void softDelete(Long id) {
        CargoSize cargoSize = cargoSizeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("CargoSize", id));
        cargoSize.markDeleted();
        cargoSizeRepository.save(cargoSize);
        log.info("货物尺寸软删除: cargoSize={}", cargoSize.getCode());
    }

    /** 升格结果 VO（简化返回） */
    public record PromoteResult(Long cargoSizeId, Long productId, String masterCode, String subCode, String nameZh) {}
}
