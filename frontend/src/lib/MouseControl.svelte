<script lang="ts">
	import { onMount, onDestroy } from 'svelte';

	interface Config {
		serverIp: string;
		serverPort: number;
	}

	let config: Config = {
		serverIp: localStorage.getItem('serverIp') || '192.168.1.100',
		serverPort: parseInt(localStorage.getItem('serverPort') || '3935')
	};

	let ws: WebSocket | null = null;
	let isConnected: boolean = false;
	let isDragging: boolean = false;

	const CIRCLE_RADIUS = 80;
	const CONTAINER_SIZE = 300;
	const THROTTLE_DELAY = 50; // Send movement updates every 50ms while dragging
	const SENSITIVITY_FACTOR = 0.05; // Range: 0.1 (slow) to 1.0 (fast), 0.4 = 40% of max speed

	let centerX: number = CONTAINER_SIZE / 2;
	let centerY: number = CONTAINER_SIZE / 2;
	let cursorX: number = 0;
	let cursorY: number = 0;
	let statusMessage: string = 'Disconnected';
	let lastSendTime: number = 0;
	let pendingCommand: Record<string, unknown> | null = null;
	let sendTimer: ReturnType<typeof setTimeout> | null = null;
	let isWaitingForResponse: boolean = false;

	function connectWebSocket(): void {
		const url = `ws://${config.serverIp}:${config.serverPort}`;

		try {
			ws = new WebSocket(url);

			ws.onopen = () => {
				isConnected = true;
				statusMessage = `Connected to ${config.serverIp}:${config.serverPort}`;
				localStorage.setItem('serverIp', config.serverIp);
				localStorage.setItem('serverPort', config.serverPort.toString());
				console.log('✅ WebSocket connected to', url);
			};

			ws.onmessage = (event: MessageEvent) => {
				const response = JSON.parse(event.data);
				console.log('📥 Server response:', response);
				isWaitingForResponse = false;

				// Send pending command if any
				if (pendingCommand && isDragging) {
					console.log('📨 Sending pending command');
					sendCommand(pendingCommand);
					pendingCommand = null;
					isWaitingForResponse = true;
				}
			};

			ws.onerror = (error) => {
				isConnected = false;
				statusMessage = `Connection error`;
				console.error('❌ WebSocket error:', error);
			};

			ws.onclose = () => {
				isConnected = false;
				statusMessage = 'Disconnected';
				console.log('🔌 WebSocket disconnected');
			};
		} catch (error) {
			isConnected = false;
			statusMessage = `Failed to connect: ${error}`;
		}
	}

	function disconnectWebSocket(): void {
		if (ws) {
			ws.close();
			ws = null;
			isConnected = false;
		}
	}

	function sendCommand(command: Record<string, unknown>): void {
		if (ws && isConnected && ws.readyState === WebSocket.OPEN) {
			const jsonCommand = JSON.stringify(command);
			console.log('📤 Event sent:', command);
			ws.send(jsonCommand);
		}
	}

	function sendCommandThrottled(command: Record<string, unknown>): void {
		// If waiting for response, queue the command
		if (isWaitingForResponse) {
			pendingCommand = command;
			console.log('⏳ Waiting for response, queueing command');
			return;
		}

		pendingCommand = command;
		const now = Date.now();
		const timeSinceLastSend = now - lastSendTime;

		// If enough time has passed, send immediately
		if (timeSinceLastSend >= THROTTLE_DELAY) {
			sendCommand(command);
			lastSendTime = now;
			pendingCommand = null;
			isWaitingForResponse = true;

			// Clear any pending timer
			if (sendTimer) {
				clearTimeout(sendTimer);
				sendTimer = null;
			}
		} else {
			// Schedule a send for the next throttle window
			if (!sendTimer) {
				sendTimer = setTimeout(() => {
					if (pendingCommand && ws && isConnected && ws.readyState === WebSocket.OPEN && !isWaitingForResponse) {
						sendCommand(pendingCommand);
						lastSendTime = Date.now();
						pendingCommand = null;
						isWaitingForResponse = true;
					}
					sendTimer = null;
				}, THROTTLE_DELAY - timeSinceLastSend);
			}
		}
	}

	function onMouseDown(event: MouseEvent): void {
		isDragging = true;
		updateCursorPosition(event);
	}

	function onMouseMove(event: MouseEvent): void {
		if (isDragging) {
			updateCursorPosition(event);
		}
	}

	function onMouseUp(): void {
		isDragging = false;
	}

	function updateCursorPosition(event: MouseEvent): void {
		const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
		const x = event.clientX - rect.left;
		const y = event.clientY - rect.top;

		const dx = x - centerX;
		const dy = y - centerY;
		const distance = Math.sqrt(dx * dx + dy * dy);

		if (distance <= CIRCLE_RADIUS) {
			cursorX = x;
			cursorY = y;

			const angle = Math.atan2(dy, dx);
			const magnitude = Math.min(distance / CIRCLE_RADIUS, 1);

			// Calculate relative movement based on direction and magnitude with sensitivity
			const baseX = Math.cos(angle) * magnitude * 100;
			const baseY = Math.sin(angle) * magnitude * 100;
			const relativeX = Math.round(baseX * SENSITIVITY_FACTOR);
			const relativeY = Math.round(baseY * SENSITIVITY_FACTOR);

			sendCommandThrottled({
				command: 'mousemove',
				dx: relativeX,
				dy: relativeY
			});
		}
	}

	function onClick(): void {
		if (isWaitingForResponse) return;
		console.log('🖱️ Left click');
		isWaitingForResponse = true;
		sendCommand({
			command: 'click',
			button: 'LEFT'
		});
	}

	function onContextMenu(event: MouseEvent): void {
		event.preventDefault();
		if (isWaitingForResponse) return;
		console.log('🖱️ Right click');
		isWaitingForResponse = true;
		sendCommand({
			command: 'click',
			button: 'RIGHT'
		});
	}

	onMount(() => {
		connectWebSocket();

		window.addEventListener('mouseup', onMouseUp);

		return () => {
			window.removeEventListener('mouseup', onMouseUp);
		};
	});

	onDestroy(() => {
		if (sendTimer) {
			clearTimeout(sendTimer);
		}
		disconnectWebSocket();
	});
</script>

<div class="min-h-screen w-full bg-gradient-to-br from-blue-900 via-blue-800 to-blue-700 flex flex-col items-center justify-center p-5 font-system">
	<!-- Header -->
	<div class="text-center mb-10">
		<h1 class="text-4xl md:text-5xl font-bold text-white mb-3">WiFi Mouse Control</h1>
		<div class="inline-block px-4 py-2 rounded-full text-sm font-medium border {isConnected
			? 'bg-green-500 bg-opacity-20 text-status-green border-status-green'
			: 'bg-red-500 bg-opacity-20 text-status-red border-status-red'}">
			{statusMessage}
		</div>
	</div>

	<!-- Connection Config Panel -->
	{#if !isConnected}
		<div class="bg-white bg-opacity-10 backdrop-blur-md rounded-xl p-8 w-full max-w-sm border border-white border-opacity-20">
			<h2 class="text-2xl font-semibold text-white mb-6">Connect to Server</h2>

			<div class="space-y-4">
				<div class="flex flex-col">
					<label for="serverIp" class="text-sm font-medium text-white mb-2">Server IP:</label>
					<input
						id="serverIp"
						type="text"
						bind:value={config.serverIp}
						placeholder="192.168.1.100"
						class="px-3 py-2 rounded-lg bg-white bg-opacity-10 border border-white border-opacity-30 text-white placeholder-white placeholder-opacity-50 focus:outline-none focus:border-opacity-60 focus:bg-opacity-15 transition-all text-sm"
					/>
				</div>

				<div class="flex flex-col">
					<label for="serverPort" class="text-sm font-medium text-white mb-2">Port:</label>
					<input
						id="serverPort"
						type="number"
						bind:value={config.serverPort}
						placeholder="3935"
						class="px-3 py-2 rounded-lg bg-white bg-opacity-10 border border-white border-opacity-30 text-white placeholder-white placeholder-opacity-50 focus:outline-none focus:border-opacity-60 focus:bg-opacity-15 transition-all text-sm"
					/>
				</div>

				<button
					on:click={connectWebSocket}
					class="w-full py-3 mt-4 bg-gradient-to-r from-purple-500 to-pink-500 hover:from-purple-600 hover:to-pink-600 text-white font-semibold rounded-lg transition-all transform hover:scale-105 active:scale-100"
				>
					Connect
				</button>
			</div>
		</div>
	{:else}
		<!-- Control Panel -->
		<div class="flex flex-col items-center gap-8">
			<!-- Circle Control -->
			<button
				class="circle-container"
				on:mousedown={onMouseDown}
				on:mousemove={onMouseMove}
				on:click={onClick}
				on:contextmenu={onContextMenu}
				type="button"
				aria-label="Mouse control pad"
				style="width: {CONTAINER_SIZE}px; height: {CONTAINER_SIZE}px;"
			>
				<svg width={CONTAINER_SIZE} height={CONTAINER_SIZE} viewBox="0 0 {CONTAINER_SIZE} {CONTAINER_SIZE}" class="drop-shadow-lg" style="width: 100%; height: 100%;">
					<!-- Background circle -->
					<circle
						cx={centerX}
						cy={centerY}
						r={CIRCLE_RADIUS}
						class="fill-white fill-opacity-10 transition-all hover:fill-opacity-15"
					/>
					<!-- Border circle -->
					<circle
						cx={centerX}
						cy={centerY}
						r={CIRCLE_RADIUS}
						class="fill-none stroke-white stroke-2"
					/>
					<!-- Cursor feedback -->
					{#if isDragging}
						<!-- Cursor line -->
						<line
							x1={centerX}
							y1={centerY}
							x2={cursorX}
							y2={cursorY}
							class="stroke-status-green stroke-2 opacity-60"
							stroke-dasharray="4,4"
						/>
						<!-- Cursor dot -->
						<circle
							cx={cursorX}
							cy={cursorY}
							r="8"
							class="fill-status-green drop-shadow-md"
							style="filter: drop-shadow(0 0 4px rgb(16, 185, 129));"
						/>
					{/if}
				</svg>
			</button>

			<!-- Instructions -->
			<div class="text-center text-sm text-white text-opacity-90 space-y-1">
				<p><strong class="text-status-green">Drag</strong> within the circle to move the mouse</p>
				<p><strong class="text-status-green">Click</strong> the circle for left click</p>
				<p><strong class="text-status-green">Right-click</strong> the circle for right click</p>
			</div>

			<!-- Disconnect Button -->
			<button
				on:click={disconnectWebSocket}
				class="px-8 py-3 bg-gradient-to-r from-red-500 to-red-600 hover:from-red-600 hover:to-red-700 text-white font-semibold rounded-lg transition-all transform hover:scale-105 active:scale-100"
			>
				Disconnect
			</button>
		</div>
	{/if}
</div>

<style>
	.circle-container {
		cursor: grab;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 50%;
		background: none;
		border: 0;
		padding: 0;
		margin: 0;
		font-family: inherit;
		transition: all 0.2s;
	}

	.circle-container:active {
		cursor: grabbing;
	}

	.circle-container:focus {
		outline: 2px solid #10b981;
		outline-offset: 2px;
	}

	:global(body) {
		margin: 0;
		padding: 0;
		overflow: hidden;
	}

	:global(*) {
		box-sizing: border-box;
	}
</style>
