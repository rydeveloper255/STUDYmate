import React from 'react';
import { VacancyItem, StudyPlanItem, UserProfile } from '../types';
import { SarkariRadarScreen } from './SarkariRadarScreen';

interface UpdatesHubScreenProps {
  user?: UserProfile;
  vacancies?: VacancyItem[];
  bookmarkedIds?: string[];
  appliedIds?: string[];
  onToggleBookmark?: (id: string) => void;
  onToggleApplied?: (id: string) => void;
  onSetTargetExam?: (examTitle: string) => void;
  onAddPlanItem?: (item: StudyPlanItem) => void;
  onStartFocusSprint?: (minutes: number, subject: string, topic: string) => void;
}

export const UpdatesHubScreen: React.FC<UpdatesHubScreenProps> = (props) => {
  return <SarkariRadarScreen {...props} />;
};

export default UpdatesHubScreen;
