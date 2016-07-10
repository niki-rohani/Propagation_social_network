package thomas.core;

import core.User;


public class FineFoodUser extends User{

		private static final long serialVersionUID = 1L;
	@SuppressWarnings("unused")
	private String userId;
	
	public FineFoodUser(String userId, String name) {
		super(name);
		this.userId=userId;
	}
	
}
