package com.example.geminibot.viewModel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.geminibot.BuildConfig
import com.example.geminibot.model.ChatModel
import com.example.geminibot.model.MessageModel
import com.example.geminibot.room.AppDatabase
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.launch

class GeminiViewModel(application: Application): AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "chat_bot"
    ).build()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",   // Grounding con Google Search funciona en 1.5-flash
        apiKey = BuildConfig.apikey,

        // ==================== SYSTEM PROMPT (Muy Importante) ====================
        systemInstruction = content {
            text("""
            Eres "Reynosa Bot", un guía turístico experto y oficial de Reynosa, Tamaulipas, México.
            
            Reglas estrictas que debes seguir siempre:
            - Solo respondes sobre temas relacionados con Reynosa, Tamaulipas.
            - Nunca respondas sobre otros lugares, ciudades o temas generales.
            - Solo respondes con texto plano (nada de imágenes, emojis excesivos ni markdown pesado).
            - Sé claro, amable, preciso y útil.
            - Usa información actualizada cuando sea necesario.
            - Si la pregunta no es sobre Reynosa, responde amablemente que solo puedes ayudar con información de Reynosa.
            
            Temas que dominas:
            • Lugares turísticos
            • Historia de Reynosa
            • Restaurantes y comida típica
            • Hoteles y hospedaje
            • Cómo llegar, transporte y seguridad
            • Eventos, ferias y festivales
            • Tips para turistas
            • Clima, mejores épocas para visitar
            • Compras, mercados y artesanías
        """.trimIndent())
        },

        // ==================== CONFIGURACIÓN ====================
        generationConfig = generationConfig {
            maxOutputTokens = 1500
            temperature = 0.6f
            topP = 0.95f
        }
    )

    private val chat by lazy {
        generativeModel.startChat()
    }

     val messageList by lazy {
         mutableStateListOf<MessageModel>()
     }


    // ROOM
    fun sendMessage(question: String) {
        viewModelScope.launch {
            try {
                // Agregar mensaje del usuario
                messageList.add(MessageModel(question, role = "user"))

                // Agregar placeholder para la respuesta
                messageList.add(MessageModel("", role = "model"))
                val lastIndex = messageList.size - 1

                var fullResponse = ""

                // ← Aquí está el streaming correcto
                chat.sendMessageStream(question).collect { chunk ->
                    chunk.text?.let { text ->
                        fullResponse += text
                        messageList[lastIndex] = MessageModel(fullResponse, role = "model")
                    }
                }

                // Guardar en Room
                val chatDao = db.chatDao()
                chatDao.insertChat(ChatModel(chat = question, role = "user"))
                chatDao.insertChat(ChatModel(chat = fullResponse, role = "model"))

            } catch (e: Exception) {
                messageList.add(MessageModel("Error: ${e.message}", role = "model"))
            }
        }
    }

    fun loadChat(){
        try {
            viewModelScope.launch {
                val chatDao = db.chatDao()
                val savedChat = chatDao.getChat()
                messageList.clear()
                for (chat in savedChat){
                    messageList.add(MessageModel(message = chat.chat, role = chat.role))
                }
            }
        }catch (e:Exception){
            messageList.add(MessageModel("Error en cargar el chat: ${e.message}", role = "model"))
        }
    }

    fun deleteChat(){
        viewModelScope.launch {
            try {
                val chatDao = db.chatDao()
                chatDao.deleteChat()
                messageList.clear()
            }catch (e:Exception){
                messageList.add(MessageModel("Error en cargar el chat: ${e.message}", role = "model"))
            }
        }
    }


}











