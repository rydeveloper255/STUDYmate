/**
 * Native Haptic Feedback & Vibrations Utility for Mobile Web & Play Store TWA
 */

export const triggerHaptic = (type: 'light' | 'medium' | 'heavy' | 'success' | 'warning' | 'error' = 'light') => {
  if (typeof window === 'undefined' || !('vibrate' in navigator)) {
    return;
  }

  try {
    switch (type) {
      case 'light':
        navigator.vibrate(12);
        break;
      case 'medium':
        navigator.vibrate(25);
        break;
      case 'heavy':
        navigator.vibrate([40, 30, 40]);
        break;
      case 'success':
        navigator.vibrate([20, 50, 20]);
        break;
      case 'warning':
        navigator.vibrate([30, 60, 30]);
        break;
      case 'error':
        navigator.vibrate([50, 40, 50, 40, 60]);
        break;
      default:
        navigator.vibrate(15);
    }
  } catch (e) {
    // Ignore if blocked by browser permission
  }
};
