import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import dev.shreyaspatil.ai.client.generativeai.type.GenerateContentResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalLayoutApi::class)
@Preview
@Composable
fun App() {
    val api = remember { GeminiApi() }
    val coroutineScope = rememberCoroutineScope()
    var prompt by remember {
        mutableStateOf(
            """
        You are an expert in nutritionist where you need to see the food items from the image
           and calculate the total calories, also provide the details of every food items with calories intake
           is below format

           1. Item 1 - no of calories
           2. Item 2 - no of calories
           ----
           ----
        Finally you can also mention whether the meal is healthy or not and also mention the
        percentage split of the ratio of carbohydrates, fats, fibers, sugar and other important
        things required in our diet
    """.trimIndent()
        )
    }

    var selectedImageData by remember { mutableStateOf<ByteArray?>(null) }
    var content by remember { mutableStateOf("") }
    var showProgress by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    var filePath by remember { mutableStateOf("") }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    val canClearPrompt by remember {
        derivedStateOf {
            prompt.isNotBlank()
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth().padding(16.dp)
        ) {
            FlowRow {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .defaultMinSize(minHeight = 52.dp),
                    label = {
                        Text("Search")
                    },
                    trailingIcon = {
                        if (canClearPrompt) {
                            IconButton(
                                onClick = { prompt = "" }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    }
                )

                OutlinedButton(
                    onClick = {
                        if (prompt.isNotBlank()) {
                            coroutineScope.launch {
                                println("prompt = $prompt")
                                content = ""
                                generateContentAsFlow(api, prompt, selectedImageData)
                                    .onStart { showProgress = true }
                                    .onCompletion { showProgress = false }
                                    .collect {
                                        println("response = ${it.text}")
                                        content += it.text
                                    }
                            }
                        }
                    },
                    enabled = prompt.isNotBlank(),
                    modifier = Modifier
                        .padding(all = 4.dp)
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                ) {
                    Text("Submit")
                }

                OutlinedButton(
                    onClick = { showImagePicker = true },
                    modifier = Modifier
                        .padding(all = 4.dp)
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                ) {
                    Text("Select Image")
                }

                ImagePicker(show = showImagePicker) { file, imageData ->
                    showImagePicker = false
                    filePath = file
                    selectedImageData = imageData
                    imageData?.let {
                        image = imageData.toComposeImageBitmap()
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            image?.let { imageBitmap ->
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = BitmapPainter(imageBitmap),
                        contentDescription = "search_image",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            if (showProgress) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Markdown(content)
            }
        }
    }
}

fun generateContentAsFlow(
    api: GeminiApi,
    prompt: String,
    imageData: ByteArray? = null
): Flow<GenerateContentResponse> = imageData?.let { imageByteArray ->
    api.generateContent(prompt, imageByteArray)
} ?: run {
    api.generateContent(prompt)
}