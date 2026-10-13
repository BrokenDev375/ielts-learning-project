package vn.edu.ptit.ielts.service;

import vn.edu.ptit.ielts.dto.AuthRequests;
import vn.edu.ptit.ielts.dto.UserView;

/** Contract injected by callers; implementation is in service.impl. */
public interface AuthService {
    UserView register(AuthRequests.Register input);
    UserView current(long id);
}
