import React from 'react';
import './index.css'; // Optional: Add your CSS file for styling

import ChatComponent from './chat/Chat'; // Adjust the path as per your folder structure

function App() {
    return (
        <div className="App">
            <header className="App-header">
                <h1>Welcome to Chat App</h1>
            </header>
            <main>
                {/* Include your ChatComponent here */}
                <ChatComponent />
            </main>
        </div>
    );
}

export default App;
