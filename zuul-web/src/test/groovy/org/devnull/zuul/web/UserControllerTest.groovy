package org.devnull.zuul.web

import org.devnull.security.model.Role
import org.devnull.security.model.User
import org.devnull.security.service.SecurityService
import org.devnull.zuul.service.ZuulService
import org.devnull.zuul.service.security.KeyConfiguration
import org.devnull.zuul.web.test.ControllerTestMixin
import org.junit.Before
import org.junit.Test

import static org.mockito.Mockito.*

@Mixin(ControllerTestMixin)
class UserControllerTest {
    UserController controller

    @Before
    void createController() {
        controller = new UserController(
                securityService: mock(SecurityService),
                zuulService: mock(ZuulService)
        )
    }

    @Test
    void shouldListAllUsersWithCorrectModelAndView() {
        def users = [new User(id: 1), new User(id: 2)]
        def roles = [new Role(id: 1), new Role(id: 2)]
        when(controller.securityService.listUsers()).thenReturn(users)
        when(controller.securityService.listRoles()).thenReturn(roles)
        def mv = controller.listUsers()
        verify(controller.securityService).listUsers()
        assert mv.viewName == "/system/users/index"
        assert mv.model.users.is(users)
        assert mv.model.roles.is(roles)
    }


}
