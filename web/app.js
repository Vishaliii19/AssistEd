// AssistEd Core Logic
document.addEventListener('DOMContentLoaded', () => {
    const statusDisplay = document.getElementById('status-display');
    const outputText = document.getElementById('output-text');
    
    // UI Buttons
    const btnVoiceNav = document.getElementById('btn-voicenav');
    const btnCaption = document.getElementById('btn-caption');
    const btnScan = document.getElementById('btn-scan');
    const btnNotes = document.getElementById('btn-notes');
    const btnSos = document.getElementById('btn-sos');
    const btnSettings = document.getElementById('btn-settings');
    
    // Speech Recognition Setup
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    let recognition = null;
    let isListening = false;
    let isCaptioning = false;

    if (SpeechRecognition) {
        recognition = new SpeechRecognition();
        recognition.continuous = false;
        recognition.interimResults = true;
        recognition.lang = 'en-US';

        recognition.onstart = () => {
            isListening = true;
            updateStatus('Listening...', true);
        };

        recognition.onend = () => {
            isListening = false;
            if (!isCaptioning) {
                updateStatus('Ready');
            } else {
                // Restart captioning if we are in caption mode
                recognition.start();
            }
        };

        recognition.onresult = (event) => {
            const transcript = event.results[event.results.length - 1][0].transcript.toLowerCase();
            outputText.textContent = transcript;

            if (!isCaptioning) {
                handleVoiceCommand(transcript);
            }
        };
    }

    function updateStatus(text, active = false) {
        statusDisplay.textContent = text;
        if (active) {
            statusDisplay.classList.add('active');
        } else {
            statusDisplay.classList.remove('active');
        }
    }

    function speak(text) {
        const utterance = new SpeechSynthesisUtterance(text);
        window.speechSynthesis.speak(utterance);
        outputText.textContent = text;
    }

    // --- Feature: Voice Navigation ---
    btnVoiceNav.addEventListener('click', () => {
        if (!recognition) return alert('Speech recognition not supported on this device.');
        
        isCaptioning = false;
        if (isListening) {
            recognition.stop();
        } else {
            speak('Say a command');
            setTimeout(() => recognition.start(), 1500);
        }
    });

    function handleVoiceCommand(command) {
        const norm = command.trim();
        
        if (norm.includes('caption')) {
            btnCaption.click();
        } else if (norm.includes('scan') || norm.includes('ocr')) {
            btnScan.click();
        } else if (norm.includes('note')) {
            btnNotes.click();
        } else if (norm.includes('sos') || norm.includes('help')) {
            btnSos.click();
        } else if (norm.includes('setting')) {
            btnSettings.click();
        }
    }

    // --- Feature: Live Caption ---
    btnCaption.addEventListener('click', () => {
        if (!recognition) return alert('Speech recognition not supported.');
        
        if (isCaptioning) {
            isCaptioning = false;
            recognition.stop();
            speak('Stopping Live Caption');
            updateStatus('Ready');
        } else {
            isCaptioning = true;
            recognition.continuous = true;
            recognition.start();
            speak('Live Captioning Started');
            updateStatus('Capturing Live...', true);
        }
    });

    // --- Feature: Scan Text (Camera) ---
    const cameraOverlay = document.getElementById('camera-overlay');
    const video = document.getElementById('camera-stream');
    
    btnScan.addEventListener('click', async () => {
        speak('Opening Camera');
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
            video.srcObject = stream;
            cameraOverlay.style.display = 'block';
        } catch (err) {
            alert('Camera access denied or not available.');
        }
    });

    document.getElementById('btn-close-camera').addEventListener('click', () => {
        const stream = video.srcObject;
        if (stream) stream.getTracks().forEach(track => track.stop());
        cameraOverlay.style.display = 'none';
        updateStatus('Ready');
    });

    document.getElementById('btn-capture').addEventListener('click', () => {
        speak('Scanning text...');
        // Mocking OCR result for now
        setTimeout(() => {
            outputText.textContent = "OCR Result: This is a sample text scanned from your device camera.";
            document.getElementById('btn-close-camera').click();
        }, 1000);
    });

    // --- Feature: My Notes ---
    btnNotes.addEventListener('click', () => {
        speak('Opening My Notes');
        const notes = JSON.parse(localStorage.getItem('assist_notes') || '[]');
        if (notes.length === 0) {
            outputText.textContent = "No notes found. You can save notes after scanning text.";
        } else {
            outputText.textContent = "Your Notes: " + notes.join(' | ');
        }
    });

    // --- Feature: SOS Alert ---
    btnSos.addEventListener('click', () => {
        speak('Emergency SOS triggered');
        const emergencyPhone = "911"; // Default
        const message = "Emergency! I need help. My location: (fetching...)";
        window.location.href = `sms:${emergencyPhone}?body=${encodeURIComponent(message)}`;
    });

    // --- Feature: Settings ---
    btnSettings.addEventListener('click', () => {
        speak('Opening Settings');
        outputText.textContent = "Settings: [Dark Mode: ON] [Voice: Female] [Language: English]";
    });
});
