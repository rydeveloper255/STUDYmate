package com.example.data.local

import com.example.data.model.NovaActionType
import com.example.data.model.NovaChatMessage
import com.example.data.model.NovaContextualAction
import com.example.data.model.NovaSender
import org.json.JSONArray
import org.json.JSONObject

object NovaConversationSerializer {

    fun serializeMessages(messages: List<NovaChatMessage>): String {
        return try {
            val jsonArray = JSONArray()
            for (msg in messages) {
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("sender", msg.sender.name)
                    put("text", msg.text)
                    put("timestamp", msg.timestamp)
                    put("actionType", msg.actionType.name)
                    if (msg.actionPayload != null) put("actionPayload", msg.actionPayload)
                    if (msg.attachedImageUri != null) put("attachedImageUri", msg.attachedImageUri)
                    if (msg.userFeedback != null) put("userFeedback", msg.userFeedback)
                    if (msg.sourceCategory != null) put("sourceCategory", msg.sourceCategory)
                    if (msg.actionButtons.isNotEmpty()) {
                        val actionsArray = JSONArray()
                        for (action in msg.actionButtons) {
                            val aObj = JSONObject().apply {
                                put("label", action.label)
                                put("iconName", action.iconName ?: "")
                                put("actionType", action.actionType.name)
                                put("payload", action.payload ?: "")
                                put("isPrimary", action.isPrimary)
                            }
                            actionsArray.put(aObj)
                        }
                        put("actionButtons", actionsArray)
                    }
                }
                jsonArray.put(obj)
            }
            jsonArray.toString()
        } catch (e: Exception) {
            "[]"
        }
    }

    fun deserializeMessages(jsonStr: String?): List<NovaChatMessage> {
        if (jsonStr.isNullOrBlank() || jsonStr == "[]") return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<NovaChatMessage>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                val senderStr = obj.optString("sender", "NOVA")
                val sender = if (senderStr == "USER") NovaSender.USER else NovaSender.NOVA
                val text = obj.optString("text", "")
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                val actTypeStr = obj.optString("actionType", "NONE")
                val actionType = try { NovaActionType.valueOf(actTypeStr) } catch(e: Exception) { NovaActionType.NONE }
                val actionPayload = if (obj.has("actionPayload")) obj.optString("actionPayload") else null
                val attachedImageUri = if (obj.has("attachedImageUri")) obj.optString("attachedImageUri") else null
                val userFeedback = if (obj.has("userFeedback")) obj.optString("userFeedback") else null
                val sourceCategory = if (obj.has("sourceCategory")) obj.optString("sourceCategory") else null

                val actionButtons = mutableListOf<NovaContextualAction>()
                if (obj.has("actionButtons")) {
                    val actsArr = obj.optJSONArray("actionButtons")
                    if (actsArr != null) {
                        for (j in 0 until actsArr.length()) {
                            val aObj = actsArr.getJSONObject(j)
                            val label = aObj.optString("label", "")
                            val iconName = aObj.optString("iconName").takeIf { it.isNotBlank() }
                            val aTypeStr = aObj.optString("actionType", "NONE")
                            val aType = try { NovaActionType.valueOf(aTypeStr) } catch(e: Exception) { NovaActionType.NONE }
                            val payload = aObj.optString("payload").takeIf { it.isNotBlank() }
                            val isPrimary = aObj.optBoolean("isPrimary", false)
                            actionButtons.add(NovaContextualAction(label, iconName, aType, payload, isPrimary))
                        }
                    }
                }

                list.add(
                    NovaChatMessage(
                        id = id,
                        sender = sender,
                        text = text,
                        timestamp = timestamp,
                        actionType = actionType,
                        actionPayload = actionPayload,
                        attachedImageUri = attachedImageUri,
                        userFeedback = userFeedback,
                        sourceCategory = sourceCategory,
                        actionButtons = actionButtons
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
