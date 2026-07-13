<script lang="ts">
	import { onMount, onDestroy } from 'svelte';

	interface Config {
		serverIp: string;
		serverPort: number;
	}

	let config: Config = {
		serverIp: localStorage.getItem('serverIp') || '192.168.0.100',
		serverPort: parseInt(localStorage.getItem('serverPort') || '3935')
	};

	let ws: WebSocket | null = null;
	let isConnected: boolean = false;
	let isDragging: boolean = false;
	let hasConnectedBefore: boolean = false;
	let userInitiatedDisconnect: boolean = false;
	let reconnectTimer: ReturnType<typeof setInterval> | null = null;

	const CIRCLE_RADIUS = 80;
	const CONTAINER_SIZE = 300;
	const RECONNECT_INTERVAL = 3000; // Reconnect every 3 seconds

	// Velocity control
	const MAX_VELOCITY = 2; // Maximum pixels per command
	const ACCELERATION = 0.8; // How quickly to reach target velocity (0-1)
	const DECELERATION = 0.9; // How quickly to stop (0-1)

	let centerX: number = CONTAINER_SIZE / 2;
	let centerY: number = CONTAINER_SIZE / 2;
	let cursorX: number = 0;
	let cursorY: number = 0;
	let statusMessage: string = 'Disconnected';
	let commandQueue: Record<string, unknown>[] = [];
	let queueProcessor: ReturnType<typeof setInterval> | null = null;
	const QUEUE_CHECK_INTERVAL = 1000;

	// Current velocity tracking
	let currentVx: number = 0;
	let currentVy: number = 0;

	function startReconnectTimer(): void {
		if (reconnectTimer) return;

		reconnectTimer = setInterval(() => {
			if (!isConnected && hasConnectedBefore && !userInitiatedDisconnect) {
				console.log('🔄 Attempting to reconnect...');
				connectWebSocket();
			}
		}, RECONNECT_INTERVAL);
	}

	function stopReconnectTimer(): void {
		if (reconnectTimer) {
			clearInterval(reconnectTimer);
			reconnectTimer = null;
		}
	}

	function connectWebSocket(): void {
		const url = `ws://${config.serverIp}:${config.serverPort}`;

		try {
			ws = new WebSocket(url);

			ws.onopen = () => {
				isConnected = true;
				hasConnectedBefore = true;
				userInitiatedDisconnect = false;
				statusMessage = `Connected to ${config.serverIp}:${config.serverPort}`;
				localStorage.setItem('serverIp', config.serverIp);
				localStorage.setItem('serverPort', config.serverPort.toString());
				stopReconnectTimer();
				startQueueProcessor();
				console.log('✅ WebSocket connected to', url);
			};

			ws.onmessage = (event: MessageEvent) => {
				const response = JSON.parse(event.data);
				console.log('📥 Server response:', response);
			};

			ws.onerror = (error) => {
				isConnected = false;
				statusMessage = `Connection error`;
				console.error('❌ WebSocket error:', error);
				if (hasConnectedBefore && !userInitiatedDisconnect) {
					startReconnectTimer();
				}
			};

			ws.onclose = () => {
				isConnected = false;
				stopQueueProcessor();
				if (!userInitiatedDisconnect) {
					statusMessage = 'Reconnecting...';
					console.log('🔌 WebSocket disconnected, attempting to reconnect');
					if (hasConnectedBefore) {
						startReconnectTimer();
					}
				} else {
					statusMessage = 'Disconnected';
					console.log('🔌 WebSocket disconnected');
				}
			};
		} catch (error) {
			isConnected = false;
			statusMessage = `Failed to connect: ${error}`;
			if (hasConnectedBefore && !userInitiatedDisconnect) {
				startReconnectTimer();
			}
		}
	}

	function disconnectWebSocket(): void {
		userInitiatedDisconnect = true;
		stopReconnectTimer();
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
			// remove command only after it was successfully sent
			commandQueue.shift();
		}
	}

	function getLastComand(): Record<string, unknown> | undefined {
		if (commandQueue.length > 0) {
			return commandQueue[commandQueue.length - 1]
		}
	}

	function shouldMergeMouseMove(command: Record<string, unknown>): boolean {
		return (
			command.command === 'mousemove' &&
			getLastComand()?.command === 'mousemove'
		)
	}

	function mergeMouseMove(command: Record<string, unknown>): void {
		const lastCommand = getLastComand();
		if (!lastCommand) return;

		const lastDx = (lastCommand.dx as number) || 0;
		const lastDy = (lastCommand.dy as number) || 0;
		let newDx = (command.dx as number) || 0;
		let newDy = (command.dy as number) || 0;

		// Calculate time interval and scale movement accordingly
		const lastTimestamp = (lastCommand.timestamp as number) || Date.now();
		const currentTimestamp = (command.timestamp as number) || Date.now();
		const timeIntervalMs = Math.max(currentTimestamp - lastTimestamp, 1); // Avoid division by zero
		const timeIntervalSec = timeIntervalMs / 1000;

		newDx *= timeIntervalSec;
		newDy *= timeIntervalSec;

		lastCommand.dx = lastDx + newDx;
		lastCommand.dy = lastDy + newDy;
		console.log('🔄 Merged move command: dx=' + (lastCommand.dx as number).toFixed(2) + ', dy=' + (lastCommand.dy as number).toFixed(2) + ', time=' + timeIntervalMs + 'ms');
	}

	function pushCommand(command: Record<string, unknown>): void {
		// Add timestamp to command
		command.timestamp = Date.now();

		// Check if last queued command is a move command and new command is also a move command
		if (shouldMergeMouseMove(command)) {
			mergeMouseMove(command);
		} else {
			commandQueue.push(command);
			console.log('📋 Command queued. Queue length:', commandQueue.length);
		}
	}

	function processQueue(): void {
		if (commandQueue.length === 0 || !isConnected) {
			return;
		}

		const command = commandQueue[0];
		if (command) {
			sendCommand(command);
		}
	}

	function startQueueProcessor(): void {
		if (queueProcessor) return;

		queueProcessor = setInterval(() => {
			processQueue();
		}, QUEUE_CHECK_INTERVAL);

		console.log('▶️ Queue processor started');
	}

	function stopQueueProcessor(): void {
		if (queueProcessor) {
			clearInterval(queueProcessor);
			queueProcessor = null;
			console.log('⏹️ Queue processor stopped');
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
		currentVx = 0;
		currentVy = 0;
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

			// Calculate target velocity based on joystick position
			const targetVx = Math.cos(angle) * magnitude * MAX_VELOCITY;
			const targetVy = Math.sin(angle) * magnitude * MAX_VELOCITY;

			// Smoothly accelerate/decelerate to target velocity
			currentVx = currentVx + (targetVx - currentVx) * ACCELERATION;
			currentVy = currentVy + (targetVy - currentVy) * ACCELERATION;

			const relativeX = Math.round(currentVx);
			const relativeY = Math.round(currentVy);

			if (relativeX !== 0 || relativeY !== 0) {
				pushCommand({
					command: 'mousemove',
					dx: relativeX,
					dy: relativeY
				});
			}
		} else {
			// Decelerate when joystick is released
			currentVx *= DECELERATION;
			currentVy *= DECELERATION;

			// Stop completely when velocity is very small
			if (Math.abs(currentVx) < 0.1) currentVx = 0;
			if (Math.abs(currentVy) < 0.1) currentVy = 0;

			if (currentVx !== 0 || currentVy !== 0) {
				const relativeX = Math.round(currentVx);
				const relativeY = Math.round(currentVy);
				pushCommand({
					command: 'mousemove',
					dx: relativeX,
					dy: relativeY
				});
			}
		}
	}

	function onClick(): void {
		console.log('🖱️ Left click');
		pushCommand({
			command: 'click',
			button: 'LEFT'
		});
	}

	function onContextMenu(event: MouseEvent): void {
		event.preventDefault();
		console.log('🖱️ Right click');
		pushCommand({
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
		stopQueueProcessor();
		stopReconnectTimer();
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
