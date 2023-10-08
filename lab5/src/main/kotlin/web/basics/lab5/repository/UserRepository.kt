package web.basics.lab5.repository

import web.basics.lab5.model.Permission
import web.basics.lab5.model.User
import java.security.MessageDigest

interface UserRepository {
    fun getUserByEmail(email: String): User?
    fun addUser(user: User)
    fun deleteUser(user: User): Boolean
    fun editUser(oldUser: User, newUser: User)
    fun getAllUsers(): List<User>
}

class UserRepositoryImpl : UserRepository {

    private fun sha256(input: String): String {
        val bytes = input.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest(bytes)

        // Convert the byte array to a hexadecimal string
        val sb = StringBuilder()
        for (hashedByte in hashedBytes) {
            sb.append(String.format("%02x", hashedByte))
        }
        return sb.toString()
    }

    private val users = hashMapOf(
        "admin@example.com" to User(
            email = "admin@example.com",
            passwordHash = sha256("password"),
            permissions = Permission.values().toSet(),
            firstName = "Admin",
            lastName = "Lastname"
        )
    )

    override fun getUserByEmail(email: String): User? {
        return users[email]
    }

    override fun addUser(user: User) {
        users[user.email] = user
    }

    override fun deleteUser(user: User): Boolean {
        return users.remove(user.email) != null
    }

    override fun editUser(oldUser: User, newUser: User) {
        if (oldUser.email == newUser.email) {
            users[oldUser.email] = newUser
        } else {
            users.remove(oldUser.email)
            users[newUser.email] = newUser
        }
    }

    override fun getAllUsers(): List<User> = users.values.toList()
}