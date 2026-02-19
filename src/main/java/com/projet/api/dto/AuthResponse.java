package com.projet.api.dto;

public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private UserDto user;

    public AuthResponse() {
    }

    public AuthResponse(String token, String type, UserDto user) {
        this.token = token;
        this.type = type;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public static AuthResponseBuilder builder() {
        return new AuthResponseBuilder();
    }

    public static class AuthResponseBuilder {
        private String token;
        private String type = "Bearer";
        private UserDto user;

        public AuthResponseBuilder token(String token) {
            this.token = token;
            return this;
        }

        public AuthResponseBuilder type(String type) {
            this.type = type;
            return this;
        }

        public AuthResponseBuilder user(UserDto user) {
            this.user = user;
            return this;
        }

        public AuthResponse build() {
            return new AuthResponse(token, type, user);
        }
    }

    public static class UserDto {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;

        public UserDto() {
        }

        public UserDto(Long id, String email, String firstName, String lastName) {
            this.id = id;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public static UserDtoBuilder builder() {
            return new UserDtoBuilder();
        }

        public static class UserDtoBuilder {
            private Long id;
            private String email;
            private String firstName;
            private String lastName;

            public UserDtoBuilder id(Long id) {
                this.id = id;
                return this;
            }

            public UserDtoBuilder email(String email) {
                this.email = email;
                return this;
            }

            public UserDtoBuilder firstName(String firstName) {
                this.firstName = firstName;
                return this;
            }

            public UserDtoBuilder lastName(String lastName) {
                this.lastName = lastName;
                return this;
            }

            public UserDto build() {
                return new UserDto(id, email, firstName, lastName);
            }
        }
    }
}
