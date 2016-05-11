package org.ibase4j.core.support.shiro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.ibase4j.core.util.WebUtil;
import org.ibase4j.mybatis.generator.model.SysUser;
import org.ibase4j.service.sys.SysPermissionService;
import org.ibase4j.service.sys.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.pagehelper.PageInfo;

public class Realm extends AuthorizingRealm {
	@Autowired
	private SysPermissionService sysPermissionService;
	@Autowired
	private SysUserService sysUserService;

	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		String currentUserAccount = (String) super.getAvailablePrincipal(principals);
		List<String> permissionList = new ArrayList<String>();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("countSql", 0);
		params.put("usable", 1);
		params.put("account", currentUserAccount);
		PageInfo<SysUser> pageInfo = sysUserService.query(params);
		if (pageInfo.getSize() == 1) {
			SysUser user = pageInfo.getList().get(0);
			// 从数据库中获取当前登录用户的详细信息
			List<String> permissions = sysPermissionService.getPermissionTypesByUserId(user.getId());
			if (null != permissions && permissions.size() > 0) {
				permissionList.addAll(permissions);
			} else {
				return null;
			}
			// 为当前用户设置角色和权限
			SimpleAuthorizationInfo simpleAuthorInfo = new SimpleAuthorizationInfo();
			simpleAuthorInfo.addStringPermissions(permissionList);
			return simpleAuthorInfo;
		}
		return null;
	}

	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authcToken)
			throws AuthenticationException {
		UsernamePasswordToken token = (UsernamePasswordToken) authcToken;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("countSql", 0);
		params.put("usable", 1);
		params.put("account", token.getUsername());
		StringBuilder sb = new StringBuilder(100);
		for (int i = 0; i < token.getPassword().length; i++) {
			sb.append(token.getPassword()[i]);
		}
		params.put("password", sb.toString());
		PageInfo<SysUser> pageInfo = sysUserService.query(params);
		if (pageInfo.getSize() == 1) {
			SysUser user = pageInfo.getList().get(0);
			WebUtil.saveCurrentUser(user.getId());
			AuthenticationInfo authcInfo = new SimpleAuthenticationInfo(user.getAccount(), user.getPassword(),
					user.getUserName());
			return authcInfo;
		} else {
			return null;
		}
	}

}