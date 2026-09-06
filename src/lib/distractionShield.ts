/**
 * StudyMate AI — Android Distraction Shield & Notification Policy Controller
 * 
 * Manages:
 * 1. Android Notification Policy / DND Interruption Controls (ACCESS_NOTIFICATION_POLICY)
 * 2. Pre-focus system state capture & idempotent post-focus restoration
 * 3. Native Android bridge communication / Intent triggers
 * 4. App crash & force-close recovery
 * 5. Screen WakeLock management during deep study sprints
 */

export type DndInterruptionFilter = 
  | 'INTERRUPTION_FILTER_ALL'      // 1: Normal (DND OFF - all notifications allowed)
  | 'INTERRUPTION_FILTER_PRIORITY' // 2: Priority (DND ON - reduces noise while keeping essential emergency contacts)
  | 'INTERRUPTION_FILTER_ALARMS'   // 4: Alarms Only
  | 'INTERRUPTION_FILTER_NONE';    // 3: Total Silence

export interface DistractionShieldState {
  isActive: boolean;
  status: 'IDLE' | 'STARTING' | 'ACTIVE' | 'ENDING' | 'RESTORED';
  sessionId?: string;
  previousInterruptionFilter: DndInterruptionFilter;
  currentInterruptionFilter: DndInterruptionFilter;
  activatedAt?: number;
  wakeLockActive: boolean;
}

export interface BlockedAppInfo {
  id: string;
  name: string;
  packageName: string;
  category: 'Social' | 'Entertainment' | 'Gaming' | 'Messaging';
  isBlocked: boolean;
  iconType: 'instagram' | 'youtube' | 'whatsapp' | 'twitter' | 'game' | 'social';
}

export const DEFAULT_BLOCKED_APPS: BlockedAppInfo[] = [
  { id: 'app_insta', name: 'Instagram & Reels', packageName: 'com.instagram.android', category: 'Social', isBlocked: true, iconType: 'instagram' },
  { id: 'app_yt', name: 'YouTube Shorts', packageName: 'com.google.android.youtube', category: 'Entertainment', isBlocked: true, iconType: 'youtube' },
  { id: 'app_wa', name: 'WhatsApp & Statuses', packageName: 'com.whatsapp', category: 'Messaging', isBlocked: false, iconType: 'whatsapp' },
  { id: 'app_x', name: 'X / Twitter Feeds', packageName: 'com.twitter.android', category: 'Social', isBlocked: true, iconType: 'twitter' },
  { id: 'app_game', name: 'BGMI / Mobile Games', packageName: 'com.pubg.imobile', category: 'Gaming', isBlocked: true, iconType: 'game' },
  { id: 'app_snap', name: 'Snapchat', packageName: 'com.snapchat.android', category: 'Social', isBlocked: true, iconType: 'social' },
];

const STORAGE_KEY_SESSION = 'studymate_distraction_shield_session';
const STORAGE_KEY_PERMISSION = 'studymate_dnd_permission_granted';
const STORAGE_KEY_CONFIG = 'studymate_distraction_shield_config';

// Active Screen WakeLock sentinel
let activeWakeLockSentinel: any = null;

// In-memory state machine guard against duplicate activation / restoration
let shieldStateMachine: DistractionShieldState = {
  isActive: false,
  status: 'IDLE',
  previousInterruptionFilter: 'INTERRUPTION_FILTER_ALL',
  currentInterruptionFilter: 'INTERRUPTION_FILTER_ALL',
  wakeLockActive: false,
};

/**
 * Checks whether Android Notification Policy Access (DND) permission is granted
 */
export const checkDndPermissionStatus = (): boolean => {
  // Check if Native Android Bridge is injected
  if (typeof window !== 'undefined' && (window as any).StudyMateAndroidBridge?.isNotificationPolicyAccessGranted) {
    try {
      return Boolean((window as any).StudyMateAndroidBridge.isNotificationPolicyAccessGranted());
    } catch (e) {
      console.warn('Error querying native Android bridge for DND access:', e);
    }
  }

  // Check stored user permission state
  if (typeof window !== 'undefined') {
    const stored = localStorage.getItem(STORAGE_KEY_PERMISSION);
    if (stored !== null) {
      return stored === 'true';
    }
  }
  return false;
};

/**
 * Requests Android Notification Policy Access by triggering the system settings intent
 */
export const requestDndAccessSettings = async (): Promise<{ success: boolean; granted: boolean }> => {
  if (typeof window === 'undefined') return { success: false, granted: false };

  // If native Android Bridge is available, call the native intent directly
  if ((window as any).StudyMateAndroidBridge?.openNotificationPolicySettings) {
    try {
      (window as any).StudyMateAndroidBridge.openNotificationPolicySettings();
      return { success: true, granted: checkDndPermissionStatus() };
    } catch (e) {
      console.warn('Native bridge openNotificationPolicySettings failed:', e);
    }
  }

  // Attempt Web Android Intent URL for DND Access Settings
  try {
    const androidIntentUrl = 'intent:#Intent;action=android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS;end';
    
    // In TWA / Android Chrome, opening intent URL triggers the system settings screen
    const isAndroid = /Android/i.test(navigator.userAgent);
    if (isAndroid) {
      window.location.href = androidIntentUrl;
    }
  } catch (e) {
    console.warn('Failed to launch android intent URL:', e);
  }

  // Update permission in local store
  localStorage.setItem(STORAGE_KEY_PERMISSION, 'true');
  return { success: true, granted: true };
};

/**
 * Manually toggle or grant DND permission (for in-app settings management)
 */
export const setDndPermissionGranted = (granted: boolean) => {
  if (typeof window !== 'undefined') {
    localStorage.setItem(STORAGE_KEY_PERMISSION, granted ? 'true' : 'false');
  }
};

/**
 * Fetches current Android system DND state
 */
export const getCurrentSystemDndFilter = (): DndInterruptionFilter => {
  if (typeof window !== 'undefined' && (window as any).StudyMateAndroidBridge?.getCurrentInterruptionFilter) {
    try {
      const filter = (window as any).StudyMateAndroidBridge.getCurrentInterruptionFilter();
      return filter || 'INTERRUPTION_FILTER_ALL';
    } catch (e) {
      console.warn('Error reading interruption filter from native bridge:', e);
    }
  }
  return shieldStateMachine.currentInterruptionFilter || 'INTERRUPTION_FILTER_ALL';
};

/**
 * Activates Distraction Shield on starting Focus Mode
 * Steps:
 * 1. Capture previous system interruption filter (e.g. ALL or PRIORITY)
 * 2. Set Android interruption filter to PRIORITY
 * 3. Request Screen WakeLock to keep timer alive
 * 4. Persist recovery state to localStorage
 */
export const activateDistractionShield = async (
  sessionId: string,
  options?: {
    allowPriorityAlarms?: boolean;
  }
): Promise<{ success: boolean; previousState: DndInterruptionFilter }> => {
  // Prevent duplicate activation if already active
  if (shieldStateMachine.isActive && shieldStateMachine.status === 'ACTIVE') {
    return {
      success: true,
      previousState: shieldStateMachine.previousInterruptionFilter,
    };
  }

  shieldStateMachine.status = 'STARTING';

  // 1. Capture previous DND state
  const previousState = getCurrentSystemDndFilter();

  // 2. Call Native Android Bridge if present
  if (typeof window !== 'undefined' && (window as any).StudyMateAndroidBridge?.setInterruptionFilter) {
    try {
      (window as any).StudyMateAndroidBridge.setInterruptionFilter('INTERRUPTION_FILTER_PRIORITY');
    } catch (e) {
      console.warn('Native bridge setInterruptionFilter failed:', e);
    }
  }

  // 3. Request Screen WakeLock
  let wakeLockSuccess = false;
  if (typeof navigator !== 'undefined' && 'wakeLock' in navigator) {
    try {
      activeWakeLockSentinel = await (navigator as any).wakeLock.request('screen');
      wakeLockSuccess = true;
    } catch (e) {
      console.info('WakeLock request declined or unsupported in this context:', e);
    }
  }

  // 4. Update in-memory state machine
  shieldStateMachine = {
    isActive: true,
    status: 'ACTIVE',
    sessionId,
    previousInterruptionFilter: previousState,
    currentInterruptionFilter: 'INTERRUPTION_FILTER_PRIORITY',
    activatedAt: Date.now(),
    wakeLockActive: wakeLockSuccess,
  };

  // 5. Persist to localStorage for crash & force-close recovery
  if (typeof window !== 'undefined') {
    localStorage.setItem(
      STORAGE_KEY_SESSION,
      JSON.stringify({
        sessionId,
        previousInterruptionFilter: previousState,
        activatedAt: Date.now(),
        isActive: true,
      })
    );
  }

  return { success: true, previousState };
};

/**
 * Deactivates Distraction Shield on Focus Mode completion / exit
 * Restores previous Android system interruption filter idempotently
 */
export const deactivateDistractionShield = async (
  reason: 'COMPLETED' | 'MANUAL_STOP' | 'STRICT_EXIT' | 'APP_RECOVERY' = 'COMPLETED'
): Promise<{ success: boolean; restoredFilter: DndInterruptionFilter }> => {
  // Idempotent guard: if not active and already idle/restored, return immediately
  if (!shieldStateMachine.isActive && shieldStateMachine.status === 'IDLE') {
    return {
      success: true,
      restoredFilter: shieldStateMachine.previousInterruptionFilter,
    };
  }

  shieldStateMachine.status = 'ENDING';
  const filterToRestore = shieldStateMachine.previousInterruptionFilter || 'INTERRUPTION_FILTER_ALL';

  // 1. Restore previous system DND filter via Native Android Bridge
  if (typeof window !== 'undefined' && (window as any).StudyMateAndroidBridge?.setInterruptionFilter) {
    try {
      (window as any).StudyMateAndroidBridge.setInterruptionFilter(filterToRestore);
    } catch (e) {
      console.warn('Native bridge restoration failed:', e);
    }
  }

  // 2. Release Screen WakeLock
  if (activeWakeLockSentinel) {
    try {
      await activeWakeLockSentinel.release();
    } catch (e) {
      // Ignore
    }
    activeWakeLockSentinel = null;
  }

  // 3. Update state machine to RESTORED / IDLE
  shieldStateMachine = {
    isActive: false,
    status: 'IDLE',
    sessionId: undefined,
    previousInterruptionFilter: filterToRestore,
    currentInterruptionFilter: filterToRestore,
    wakeLockActive: false,
  };

  // 4. Remove session from localStorage
  if (typeof window !== 'undefined') {
    localStorage.removeItem(STORAGE_KEY_SESSION);
  }

  return { success: true, restoredFilter: filterToRestore };
};

/**
 * App Launch & Crash Recovery Logic:
 * Detects if the app was force closed or restarted during an active focus sprint,
 * and safely restores previous system notification/DND state so the user's phone
 * is never permanently stuck in DND mode!
 */
export const recoverDistractionShieldOnAppLaunch = (): { recovered: boolean; previousFilter?: DndInterruptionFilter } => {
  if (typeof window === 'undefined') return { recovered: false };

  try {
    const rawSession = localStorage.getItem(STORAGE_KEY_SESSION);
    if (rawSession) {
      const parsed = JSON.parse(rawSession);
      if (parsed && parsed.isActive) {
        const previousFilter: DndInterruptionFilter = parsed.previousInterruptionFilter || 'INTERRUPTION_FILTER_ALL';

        // Call native bridge to restore original state
        if ((window as any).StudyMateAndroidBridge?.setInterruptionFilter) {
          (window as any).StudyMateAndroidBridge.setInterruptionFilter(previousFilter);
        }

        // Clean up stale session
        localStorage.removeItem(STORAGE_KEY_SESSION);

        shieldStateMachine = {
          isActive: false,
          status: 'IDLE',
          previousInterruptionFilter: previousFilter,
          currentInterruptionFilter: previousFilter,
          wakeLockActive: false,
        };

        return { recovered: true, previousFilter };
      }
    }
  } catch (e) {
    console.warn('Failed to parse or recover distraction shield session:', e);
    localStorage.removeItem(STORAGE_KEY_SESSION);
  }

  return { recovered: false };
};

/**
 * Current State Getter
 */
export const getDistractionShieldState = (): DistractionShieldState => {
  return { ...shieldStateMachine };
};
