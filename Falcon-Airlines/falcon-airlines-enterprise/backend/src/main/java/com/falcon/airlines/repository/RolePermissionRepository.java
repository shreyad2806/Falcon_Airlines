package com.falcon.airlines.repository;

import com.falcon.airlines.entity.Role;
import com.falcon.airlines.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long>, JpaSpecificationExecutor<RolePermission> {

    List<RolePermission> findByRoleIn(Collection<Role> roles);
}
