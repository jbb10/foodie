package com.foodie.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foodie.app.ui.theme.FoodieTheme

/**
 * Activity that displays the privacy policy and permissions rationale for Health Connect.
 *
 * This activity is required by Health Connect to explain to users why the app needs
 * access to health data and how that data will be used.
 *
 * It is launched when users click the privacy policy link in the Health Connect
 * permission screen.
 */
class HealthConnectPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FoodieTheme {
                PermissionsRationaleScreen(
                    onBackClick = { finish() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionsRationaleScreen(
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Health Connect Permissions",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Text(
                text = "Why Foodie Needs Health Connect Access",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text = "Foodie requests permission to read and write nutrition data in Health Connect to provide you with a seamless meal tracking experience.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Text(
                text = "What Data We Access",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text = """
                    • Nutrition Records: We write meal information (calories, description, timestamp) to Health Connect after you capture and analyze a meal photo.

                    • Read Access: We read your existing nutrition records to display your meal history within the app.
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Text(
                text = "How We Use Your Data",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text = """
                    1. Photo Analysis: When you capture a meal photo, we send it to Azure OpenAI Vision API to analyze the food and estimate nutritional content.

                    1. Camera: Used only to take photos of your meals. Photos are processed locally and by our secure AI partner, then deleted immediately.

                    2. Health Connect Storage: The analysed nutrition data (calories and meal description) is saved to your Health Connect database, where it can be accessed by other health apps you choose to connect.

                    3. Notifications: Used to show you the progress of meal analysis so you can pocket your phone immediately after snapping a photo.

                    We do NOT:
                    • Store your health data on our servers
                    • Share your nutrition data with third parties
                    • Use your data for advertising
                    • Access any health data beyond nutrition records
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Text(
                text = "Your Privacy Rights",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text = """
                    • You can revoke Health Connect permissions at any time through Android Settings → Health Connect → App permissions → Foodie

                    • All nutrition data is stored locally on your device in Health Connect, not on external servers

                    • You have full control over which apps can access your Health Connect data

                    • Deleting the Foodie app will not delete your Health Connect data; you must delete it separately through the Health Connect app
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Text(
                text = "Data Security",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text = """
                    • Meal photos are temporarily stored in the app's cache directory and deleted immediately after analysis

                    • API communication with Azure OpenAI uses encrypted HTTPS connections

                    • Your Azure OpenAI API key is stored securely using Android's EncryptedSharedPreferences

                    • Health Connect data is protected by Android's built-in security and permission system
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Text(
                text = "Questions or Concerns?",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text = "This is a personal project for meal tracking. If you have any questions about how your data is handled, please review the app's open-source code or contact the developer.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
