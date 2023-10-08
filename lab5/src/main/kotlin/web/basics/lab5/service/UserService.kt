package web.basics.lab5.service

import web.basics.lab5.model.Permission
import web.basics.lab5.model.User
import web.basics.lab5.repository.UserRepository

class UserService(private val userRepository: UserRepository) {

    fun login(email: String, passwordHash: String): Boolean =
        userRepository.getUserByEmail(email)?.passwordHash == passwordHash

    fun isEmailAvailable(email: String): Boolean = userRepository.getUserByEmail(email) == null

    fun register(email: String, passwordHash: String, firstName: String, lastName: String) {
        userRepository.addUser(
            User(
                email,
                passwordHash,
                setOf(
                    Permission.GET_INFO_ABOUT_OWN_ACCOUNT,
                    Permission.CHANGE_OWN_NAME,
                    Permission.CHANGE_OWN_PASSWORD
                ),
                firstName,
                lastName
            )
        )
    }

    fun getUserByEmail(email: String) = userRepository.getUserByEmail(email)

    fun getAllUsers() = userRepository.getAllUsers()

    fun grantPermission(user: User, permission: Permission) {
        userRepository.editUser(user, user.copy(permissions = user.permissions.plus(permission)))
    }

    fun revokePermission(user: User, permission: Permission) {
        userRepository.editUser(user, user.copy(permissions = user.permissions.minus(permission)))
    }

    fun changePassword(user: User, oldPasswordHash: String, newPasswordHash: String): Boolean {
        return if (user.passwordHash == oldPasswordHash) {
            userRepository.editUser(user, user.copy(passwordHash = newPasswordHash))
            true
        } else {
            false
        }
    }

    fun changeEmail(user: User, hashedPassword: String, newEmail: String): Boolean {
        return if (user.passwordHash == hashedPassword) {
            userRepository.editUser(user, user.copy(email = newEmail))
            true
        } else {
            false
        }
    }

    fun changeFirstName(user: User, newFirstName: String) {
        userRepository.editUser(user, user.copy(firstName = newFirstName))
    }

    fun changeLastName(user: User, newLastName: String) {
        userRepository.editUser(user, user.copy(lastName = newLastName))
    }
}