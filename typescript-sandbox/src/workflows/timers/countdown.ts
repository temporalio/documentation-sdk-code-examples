import { UpdatableTimer } from './updatable-timer';

// @@@SNIPSTART typescript-timers-updatable-timer-usage
import * as wf from '@temporalio/workflow';

export const setDeadlineSignal = wf.defineSignal<[number]>('setDeadline');
export const timeLeftQuery = wf.defineQuery<number>('timeLeft');

// usage
export async function countdownWorkflow(): Promise<void> {
  const target = Date.now() + 24 * 60 * 60 * 1000; // 1 day!!!
  const timer = new UpdatableTimer(target);
  console.log('timer set for: ' + new Date(target).toString());
  wf.setHandler(setDeadlineSignal, (deadline) => {
    // send in new deadlines via Signal
    timer.deadline = deadline;
    console.log('timer now set for: ' + new Date(deadline).toString());
  });
  wf.setHandler(timeLeftQuery, () => timer.deadline - Date.now());
  await timer; // if you send in a signal with a new time, this timer will resolve earlier!
  console.log('countdown done!');
}
// @@@SNIPEND
