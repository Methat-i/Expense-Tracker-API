package com.example

import com.asyncapi.kotlinasyncapi.context.service.AsyncApiExtension
import com.asyncapi.kotlinasyncapi.ktor.AsyncApiPlugin
import com.example.TaskRepository.tasks
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable


@Serializable
data class Task(val id: Int, val content: String, val isDone: Boolean)

data class TaskRequest(val content: String, val isDone: Boolean)

object TaskRepository {
    val tasks = mutableListOf<Task>(
        Task(id = 1, content = "Learn Ktor", isDone = true),
        Task(id = 2, content = "Build a REST API", isDone = false),
        Task(id = 3, content = "Write Unit Tests", isDone = false)
    )

    fun getAll(): List<Task> {
        return tasks
    }

    fun getById(id: Int): Task {
        return tasks.first { it.id == id }
    }

    fun add(task: Task): Task {
        tasks.add(task)
        return task

    }

    fun update(id: Int, updateTask: Task): Task? {
        val index = tasks.indexOfFirst { it.id == id }
        if (index == -1) return null
        tasks[index] = tasks[index].copy(content = updateTask.content, isDone = updateTask.isDone)
        return tasks[index]
    }

    fun delete(id: Int): Task {
        val idRemove = tasks.firstOrNull { it.id == id }
            ?: throw NoSuchElementException("Task with ID $id not found")
        tasks.remove(idRemove)
        return idRemove
    }


}

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello Methat")
        }
        get("/tasks") {
            val tasks = TaskRepository.getAll()
            call.respond(tasks)

        }
        get("/tasks/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.NotFound)

            val task = TaskRepository.getById(id.toInt()) ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(task)
        }
        post("/tasks") {
            val taskRequest = call.receive<TaskRequest>()
            val newId = TaskRepository.tasks.maxOfOrNull { it.id }?.plus(1) ?: 1
            val newTask = Task(id = newId, content = taskRequest.content, isDone = taskRequest.isDone)
            TaskRepository.add(newTask)
            call.respond(HttpStatusCode.Created, newTask)
        }
        put("/tasks/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, "Invalid ID")

            val task = TaskRepository.tasks.firstOrNull { it.id == id }
                ?: return@put call.respond(HttpStatusCode.NotFound, "Task not found")
            val taskRequest = call.receive<TaskRequest>()
            val updatedTask = Task(id.toInt(), taskRequest.content, taskRequest.isDone)
            TaskRepository.update(id.toInt(), updatedTask)
            call.respond(HttpStatusCode.OK, updatedTask)

        }

        delete("/tasks/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid ID")

            val task = TaskRepository.tasks.firstOrNull { it.id == id }
                ?: return@delete call.respond(HttpStatusCode.NotFound, "Task not found")

            TaskRepository.delete(id.toInt()) ?: return@delete call.respond(HttpStatusCode.NoContent)
        }
    }
}